package com.zincoid.me.module.ai.message;

import lombok.Getter;

@Getter
public enum AiRole {

    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    private final String value;

    AiRole(String value) {
        this.value = value;
    }
}
