package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Perm {

    ARTICLE_OP(0);

    @EnumValue
    private final Integer value;

    Perm(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static Perm fromValue(Integer value) {
        if (value == null) return null;
        for (Perm r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
