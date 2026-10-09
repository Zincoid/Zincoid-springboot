package com.zincoid.me.ai.client;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiState {

    RUNNING("running"),
    DONE("done");

    private final String value;

    AiState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
