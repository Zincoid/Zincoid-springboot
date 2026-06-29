package com.zincoid.me.service;

public interface EmailService {

    void sendCode(String toEmail);

    boolean verify(String email, String code);
}
