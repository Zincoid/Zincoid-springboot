package com.zincoid.me.model.enums.type;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum BizType {

    MOMENT(0),
    ARTICLE(1),
    AVATAR(2),
    CHAT(3),
    REPO(4),
    MUSIC(5),
    COMMENT(6);

    @EnumValue
    private final Integer value;

    BizType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static BizType fromValue(Integer value) {
        if (value == null) return null;
        for (BizType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
