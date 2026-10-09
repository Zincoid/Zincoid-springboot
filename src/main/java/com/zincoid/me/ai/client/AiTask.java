package com.zincoid.me.ai.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiTask {

    CHAT("Chat"),
    COMMENT("Comment");

    private final String value;

    AiTask(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AiTask fromValue(String value) {
        if (value == null) return null;
        for (AiTask t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }
}
