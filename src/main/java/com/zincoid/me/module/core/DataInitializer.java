package com.zincoid.me.module.core;

import com.zincoid.me.model.Conf;
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
        for (Conf conf : Conf.values()) initConfig(conf);
    }

    private void initConfig(Conf conf) {
        if (configService.lambdaQuery().eq(Config::getConfigKey, conf.getKey()).exists()) return;
        configService.save(Config.builder()
                .configKey(conf.getKey())
                .configValue(conf.getDefaultValue())
                .description(conf.getDescription())
                .build());
        log.info("Config created: {} = {}", conf.getKey(), conf.getDefaultValue());
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
                .bio("""
                        在聊天或评论区中，@此账号可与AI对话。
                        In Chats or Comments, @ this account to chat with AI.""")
                .skills("[\"Chat\", \"Comment\", \"Content Retrieval\", \"Web Search\"]")
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
