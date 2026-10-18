package com.zincoid.me.service.impl;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.Error;
import com.zincoid.me.model.enums.RepoAccessType;
import com.zincoid.me.model.Captcha;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.vo.UserConfigVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.UserConfigService;
import com.zincoid.me.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final int CODE_TTL = 5 * 60 * 1000; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;
    private final UserService userService;
    private final UserConfigService userConfigService;

    @Value("${spring.mail.username}")
    private String from;
    @Value("${site.name}")
    private String siteName;
    @Value("${site.url}")
    private String siteUrl;

    private record CodeEntry(String code, long expiresAt, Captcha type) {}

    @Override
    public void sendRegisterCode(String email) {
        if (email == null || email.isBlank())
            throw new BusinessException(Error.EMAIL_REQUIRED);
        if (userService.lambdaQuery().eq(User::getEmail, email).exists())
            throw new BusinessException(Error.EMAIL_ALREADY_REGISTERED);
        sendCode(email, Captcha.REGISTER);
    }

    @Override
    public void sendResetCode(String email) {
        if (!userService.lambdaQuery().eq(User::getEmail, email).exists())
            throw new BusinessException(Error.EMAIL_NOT_REGISTERED);
        sendCode(email, Captcha.RESET_PASSWORD);
    }

    @Override
    public void sendChangeCode(String email) {
        if (email == null || email.isBlank())
            throw new BusinessException(Error.EMAIL_REQUIRED);
        if (userService.lambdaQuery().eq(User::getEmail, email).exists())
            throw new BusinessException(Error.EMAIL_ALREADY_REGISTERED);
        sendCode(email, Captcha.CHANGE_EMAIL_NEW);
    }

    @Override
    public void sendChangeCode(Long userId) {
        User user = userService.getById(userId);
        if (user == null)
            throw new BusinessException(Error.ACCOUNT_NOT_FOUND);
        if (user.getEmail() == null || user.getEmail().isBlank())
            throw new BusinessException(Error.NO_EMAIL_SET);
        sendCode(user.getEmail(), Captcha.CHANGE_EMAIL_OLD);
    }

    @Override
    public boolean verifyCode(String email, String code, Captcha type, boolean remove) {
        if (email == null || code == null) return false;
        CodeEntry entry = codes.get(email);
        if (entry == null || System.currentTimeMillis() > entry.expiresAt) {
            codes.remove(email);
            return false;
        }
        if (entry.type != type) return false;
        if (entry.code.equals(code)) {
            if (remove) codes.remove(email);
            return true;
        }
        return false;
    }

    @Override
    public void removeCode(String email, Captcha type) {
        CodeEntry entry = codes.get(email);
        if (entry != null && entry.type == type)
            codes.remove(email);
    }

    @Override
    public void sendEmail(String to, String subject, String text) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(text);
                mailSender.send(msg);
                log.info("Email sent to {}", to);
            } catch (Exception e) {
                log.error("Failed to send email to {}", to, e);
            }
        });
    }

    @Override
    public void sendBroadcast(String subject, String content, boolean force) {
        List<User> users = userService.lambdaQuery()
                .eq(User::getStatus, Status.ACTIVE)
                .list();
        int count = 0;
        for (User user : users) {
            if (user.getEmail() == null || user.getEmail().isBlank()) continue;
            if (!force) {
                UserConfigVO config = userConfigService.get(user.getId());
                if (!(config.getReceiveEmail() && config.getReceiveEmailSys())) continue;
            }
            sendEmail(user.getEmail(), subject, content);
            count++;
        }
        log.info("Email broadcast sent: subject={}, force={}, recipients={}", subject, force, count);
    }

    @Override
    public void sendAccessApproved(Long userId, String repoName, RepoAccessType type) {
        User user = userService.getById(userId);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        UserConfigVO config = userConfigService.get(userId);
        if (!(config.getReceiveEmail() && config.getReceiveEmailRepoAccess())) return;
        String subject = "Zincoid's - Repo Access Approved";
        String text = """
                Your access to repo "%s" has been approved.
                Your access type is %s.

                ══════ %s (%s) ══════""".formatted(repoName, type, siteName, siteUrl);
        sendEmail(user.getEmail(), subject, text);
    }

    // ──────── Private tool ────────────────────────────────

    private void sendCode(String email, Captcha type) {
        CodeEntry existing = codes.get(email);
        if (existing != null && existing.type == type && existing.expiresAt > System.currentTimeMillis())
            throw new BusinessException(Error.VERIFICATION_CODE_RESEND_LIMIT);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(email, new CodeEntry(code, System.currentTimeMillis() + CODE_TTL, type));
        log.info("Verification code generated for {} (type={})", email, type);
        String subject = switch (type) {
            case REGISTER -> "Zincoid's - Registration Verification";
            case RESET_PASSWORD -> "Zincoid's - Password Reset Verification";
            case CHANGE_EMAIL_OLD, CHANGE_EMAIL_NEW -> "Zincoid's - Email Change Verification";
        };
        String purpose = switch (type) {
            case REGISTER -> "register your account";
            case RESET_PASSWORD -> "reset your password";
            case CHANGE_EMAIL_OLD, CHANGE_EMAIL_NEW -> "change your email";
        };
        String text = """
                Your verification code is: %s

                Use this code to %s.
                This code expires in 5 minutes.
                
                ══════ %s (%s) ══════""".formatted(code, purpose, siteName, siteUrl);
        sendEmail(email, subject, text);
    }
}
