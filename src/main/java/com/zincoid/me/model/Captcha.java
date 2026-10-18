package com.zincoid.me.model;

import lombok.Getter;

@Getter
public enum Captcha {

    REGISTER,
    RESET_PASSWORD,
    CHANGE_EMAIL_OLD,
    CHANGE_EMAIL_NEW
}
