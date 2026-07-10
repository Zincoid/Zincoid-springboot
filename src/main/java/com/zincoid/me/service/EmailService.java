package com.zincoid.me.service;

import com.zincoid.me.model.enums.CodeType;

public interface EmailService {

    void sendRegisterCode(String email);

    void sendResetCode(String email);

    void sendChangeCode(String email);

    void sendChangeCode(Long userId);

    boolean verify(String email, String code, CodeType type, boolean remove);

    void remove(String email, CodeType type);
}
