package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RelatedType {

    MOMENT(0),
    ARTICLE(1),
    AVATAR(2),
    CHAT(3);

    @EnumValue
    private final Integer value;

    RelatedType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static RelatedType fromValue(Integer value) {
        if (value == null) return null;
        for (RelatedType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
