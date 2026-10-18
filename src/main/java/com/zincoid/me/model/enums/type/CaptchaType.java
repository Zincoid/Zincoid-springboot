package com.zincoid.me.model.enums.type;

import lombok.Getter;

@Getter
public enum CaptchaType {

    REGISTER,
    RESET_PASSWORD,
    CHANGE_EMAIL_OLD,
    CHANGE_EMAIL_NEW
}
