package com.zincoid.me.service;

import com.zincoid.me.model.enums.CodeType;

public interface EmailService {

    void sendRegisterCode(String email);

    void sendResetCode(String email);

    void sendChangeCode(String email);

    void sendChangeCode(Long userId);

    boolean verifyCode(String email, String code, CodeType type, boolean remove);

    void removeCode(String email, CodeType type);

    void sendEmail(String to, String subject, String text);

    void sendBroadcast(String subject, String content, boolean force);
}
