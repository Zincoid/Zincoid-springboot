package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Role {

    USER(0),
    ADMIN(1);

    @EnumValue
    private final Integer value;

    Role(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Role fromValue(Integer value) {
        if (value == null) return null;
        for (Role r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
