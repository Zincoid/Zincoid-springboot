package com.zincoid.me.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.chat")
public class AiChatProperties {

    private int maxLength = 20;
    private int maxTokens = 1024;
    private int maxTcs = 5;
}
