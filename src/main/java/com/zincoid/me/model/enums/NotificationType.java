package com.zincoid.me.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NotificationType {

    COMMENT(0),
    REPLY(1),
    MOMENT_MENTION(2),
    COMMENT_MENTION(3),
    CHAT_MENTION(4);

    @EnumValue
    private final Integer value;

    NotificationType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationType fromValue(Integer value) {
        if (value == null) return null;
        for (NotificationType r : values()) {
            if (r.value.equals(value)) return r;
        }
        return null;
    }
}
