package com.zincoid.me.ai.message;

import lombok.Getter;

@Getter
public enum MsgRole {

    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    private final String value;

    MsgRole(String value) {
        this.value = value;
    }
}
