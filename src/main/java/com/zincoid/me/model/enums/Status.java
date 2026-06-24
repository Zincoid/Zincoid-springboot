package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Status {

    DISABLED(0),
    ACTIVE(1);

    @EnumValue
    private final Integer value;

    Status(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Status fromValue(Integer value) {
        if (value == null) return null;
        for (Status s : values()) {
            if (s.value.equals(value)) return s;
        }
        return null;
    }
}
