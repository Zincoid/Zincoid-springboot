package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Gender {

    MALE(0),
    FEMALE(1);

    @EnumValue
    private final Integer value;

    Gender(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Gender fromValue(Integer value) {
        if (value == null) return null;
        for (Gender g : values()) {
            if (g.value.equals(value)) return g;
        }
        return null;
    }
}
