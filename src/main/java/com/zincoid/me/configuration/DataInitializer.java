package com.zincoid.me.configuration;

import com.zincoid.me.model.po.Config;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.po.UserConfig;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.UserConfigService;
import com.zincoid.me.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    public static final String AI_USERNAME = "ai";

    private final UserService userService;
    private final ConfigService configService;
    private final UserConfigService userConfigService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String @NonNull ... args) {
        initAdminUser();
        initAiUser();
        initConfigs();
        initUserConfigs();
    }

    private void initConfigs() {
        initConfig("site_name", "Zincoid's", "Website name");
        initConfig("site_desc_en", "Personal website and blogs", "Website description (English)");
        initConfig("site_desc_zh", "个人网站与博客", "Website description (Chinese)");
        initConfig("page_size", "10", "Default pagination page size");
        initConfig("message_max_count", "100", "Maximum number of messages to keep");
        initConfig("loading_spinner_hold", "250", "Loading spinner hold duration (ms) before fade");
        initConfig("loading_spinner_fade", "125", "Loading spinner fade-out duration (ms)");
        initConfig("hero_animation", "random", "Hero animation: squares, raindrop, raindrop_sin, or random");
        initConfig("audio_spectrum_ratio", "0.2", "Audio spectrum ratio of digital flow for Walkman");
        initConfig("maintenance_enabled", "true", "Enable daily maintenance (UTC+8 00:00-00:10, auto cleanup and block all requests)");
        initConfig("ai_chat_prompt", "你是 Zincoid 网站聊天室里的 AI 助手。请自然地融入对话，回复简洁友好，不要长篇大论。", "AI chat system prompt");
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
            log.info("Admin already exists, skipping init.");
            return;
        }
        String password = UUID.randomUUID().toString();
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(password))
                .nickname("admin")
                .role(Role.ADMIN)
                .title("Founder")
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userService.save(admin);
        log.info("Default admin created (username: admin, password: {})", password);
    }

    private void initAiUser() {
        if (userService.lambdaQuery().eq(User::getUsername, AI_USERNAME).exists()) {
            log.info("AI already exists, skipping init.");
            return;
        }
        User ai = User.builder()
                .username(AI_USERNAME)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .nickname("AI")
                .role(Role.ADMIN)
                .title("AI Assistant")
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userService.save(ai);
        log.info("Default AI created (username: {}, id: {})", AI_USERNAME, ai.getId());
    }

    private void initUserConfigs() {
        int count = 0;
        for (User user : userService.list()) {
            if (userConfigService.lambdaQuery()
                    .eq(UserConfig::getUserId, user.getId()).exists())
                continue;
            userConfigService.create(user.getId());
            count++;
        }
        if (count > 0) log.info("Default user configs created: {}", count);
    }
}
