package com.zincoid.me.configuration;

import com.zincoid.me.service.CleanupService;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.RequestService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Maintainer {

    private static final String CONFIG_KEY = "maintenance_enabled";
    private static final int AUTO_CLEANUP_REQUEST_RETENTION_DAYS = 7;

    private final ConfigService configService;
    private final CleanupService cleanupService;

    @Getter
    private volatile boolean active = false;

    @Scheduled(cron = "0 0 0 * * *")
    public void start() {
        if (!isEnabled()) return;
        active = true;
        log.info("Maintenance started, req blocked");
        runCleanup();
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void end() {
        if (!active) return;
        active = false;
        log.info("Maintenance ended, req resumed");
    }

    private void runCleanup() {
        try {
            cleanupService.cleanupRecords();
            cleanupService.cleanupFiles(true);
            cleanupService.cleanupExpiredRequests(
                    AUTO_CLEANUP_REQUEST_RETENTION_DAYS);
            log.info("Maintenance cleanup done");
        } catch (Exception e) {
            log.error("Maintenance cleanup failed", e);
        }
    }

    private boolean isEnabled() {
        String value = configService.get(CONFIG_KEY);
        return "true".equalsIgnoreCase(value);
    }
}
