package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RequestType {

    STORAGE_EXTENSION(0),
    REPORT(1),
    MUSIC_REQUEST(2),
    REPO_TRANSFER(3);

    @EnumValue
    private final Integer value;

    RequestType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static RequestType fromValue(Integer value) {
        if (value == null) return null;
        for (RequestType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
