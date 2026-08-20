package com.zincoid.me.configuration;

import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.impl.CleanupServiceImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceManager {

    private static final String CONFIG_KEY = "maintenance_enabled";

    private final ConfigService configService;
    private final CleanupServiceImpl cleanupService;

    @Getter
    private volatile boolean active = false;

    @Scheduled(cron = "0 0 0 * * *")
    public void start() {
        if (!isEnabled()) return;
        active = true;
        log.info("Maintenance window started, blocking all requests");
        runCleanup();
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void end() {
        if (!active) return;
        active = false;
        log.info("Maintenance window ended, resuming all requests");
    }

    private void runCleanup() {
        try {
            var records = cleanupService.cleanupRecords();
            var files = cleanupService.cleanupFiles(false);
            log.info("Maintenance cleanup done: records={}, files={}", records, files);
        } catch (Exception e) {
            log.error("Maintenance cleanup failed", e);
        }
    }

    private boolean isEnabled() {
        return "true".equalsIgnoreCase(configService.get(CONFIG_KEY));
    }
}
