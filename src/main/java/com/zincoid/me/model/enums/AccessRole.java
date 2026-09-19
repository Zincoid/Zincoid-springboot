package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AccessRole {

    VIEWER(0),
    CONTRIBUTOR(1);

    @EnumValue
    private final Integer value;

    AccessRole(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static AccessRole fromValue(Integer value) {
        if (value == null) return null;
        for (AccessRole r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
