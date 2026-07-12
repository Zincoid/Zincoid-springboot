package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Access {

    PENDING(0),
    APPROVED(1),
    REJECTED(2);

    @EnumValue
    private final Integer value;

    Access(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Access fromValue(Integer value) {
        if (value == null) return null;
        for (Access s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}
