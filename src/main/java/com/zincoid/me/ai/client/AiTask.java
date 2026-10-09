package com.zincoid.me.ai.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiTask {

    CHAT("chat"),
    COMMENT("comment");

    private final String value;

    AiTask(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
