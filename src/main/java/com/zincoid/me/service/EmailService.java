package com.zincoid.me.service;

import com.zincoid.me.model.enums.RepoAccessType;
import com.zincoid.me.model.enums.CaptchaType;

public interface EmailService {

    void sendRegisterCode(String email);

    void sendResetCode(String email);

    void sendChangeCode(String email);

    void sendChangeCode(Long userId);

    boolean verifyCode(String email, String code, CaptchaType type, boolean remove);

    void removeCode(String email, CaptchaType type);

    void sendEmail(String to, String subject, String text);

    void sendBroadcast(String subject, String content, boolean force);

    void sendAccessApproved(Long userId, String repoName, RepoAccessType type);
}
