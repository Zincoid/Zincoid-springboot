package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Permission {

    ARTICLE_OP(0);

    @EnumValue
    private final Integer value;

    Permission(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Permission fromValue(Integer value) {
        if (value == null) return null;
        for (Permission r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
