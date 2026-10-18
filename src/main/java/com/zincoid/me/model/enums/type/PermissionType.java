package com.zincoid.me.model.enums.type;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PermissionType {

    ARTICLE_OP(0);

    @EnumValue
    private final Integer value;

    PermissionType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static PermissionType fromValue(Integer value) {
        if (value == null) return null;
        for (PermissionType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
