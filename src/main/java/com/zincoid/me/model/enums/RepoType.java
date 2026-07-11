package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RepoType {

    CODE(0),
    MEDIA(1),
    FILE(2);

    @EnumValue
    private final Integer value;

    RepoType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static RepoType fromValue(Integer value) {
        if (value == null) return null;
        for (RepoType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
