package com.zincoid.me.configuration;

import com.zincoid.me.model.po.Config;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final ConfigService configService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String @NonNull ... args) {
        initAdminUser();
        initConfigs();
    }

    private void initConfigs() {
        initConfig("site_name", "Zincoid's", "Website name");
        initConfig("site_desc_en", "Personal website and blog", "Website description (English)");
        initConfig("site_desc_zh", "个人网站与博客", "Website description (Chinese)");
        initConfig("page_size", "10", "Default pagination page size");
        initConfig("message_max_count", "100", "Maximum number of messages to keep");
        initConfig("loading_spinner_hold", "250", "Loading spinner hold duration (ms) before fade");
        initConfig("loading_spinner_fade", "125", "Loading spinner fade-out duration (ms)");
    }

    private void initConfig(String key, String value, String description) {
        if (configService.get(key) == null) {
            configService.save(Config.builder()
                    .configKey(key)
                    .configValue(value)
                    .description(description)
                    .build());
            log.info("Config created: {} = {}", key, value);
        }
    }

    private void initAdminUser() {
        if (userService.lambdaQuery().eq(User::getRole, Role.ADMIN).exists()) {
            log.info("Admin user already exists, skipping init.");
            return;
        }
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .nickname("admin")
                .role(Role.ADMIN)
                .title("Founder")
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userService.save(admin);
        log.info("Default admin user created (username: admin, password: admin)");
    }
}
