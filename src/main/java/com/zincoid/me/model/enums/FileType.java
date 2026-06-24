package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FileType {

    IMAGE(0),
    VIDEO(1),
    AUDIO(2),
    OTHER(3);

    @EnumValue
    private final Integer value;

    FileType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static FileType fromValue(Integer value) {
        if (value == null) return null;
        for (FileType f : values()) {
            if (f.value.equals(value)) return f;
        }
        return null;
    }
}
