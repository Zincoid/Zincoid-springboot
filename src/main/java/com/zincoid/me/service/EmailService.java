package com.zincoid.me.service;

public interface EmailService {

    void sendRegisterCode(String email);

    void sendResetCode(String email);

    boolean verify(String email, String code);
}
