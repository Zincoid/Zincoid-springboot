package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Visibility {

    PUBLIC(0),
    PRIVATE(1),
    RESTRICTED(2);

    @EnumValue
    private final Integer value;

    Visibility(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Visibility fromValue(Integer value) {
        if (value == null) return null;
        for (Visibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        return null;
    }
}
