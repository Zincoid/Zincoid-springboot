package com.zincoid.me.model.enums.type;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AccessType {

    VIEWER(0),
    CONTRIBUTOR(1);

    @EnumValue
    private final Integer value;

    AccessType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static AccessType fromValue(Integer value) {
        if (value == null) return null;
        for (AccessType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
