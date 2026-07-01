package com.zincoid.me.service.impl;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.CodeType;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final int CODE_TTL = 5 * 60 * 1000; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, CodeEntry> codes = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;
    private final String from;
    private final UserService userService;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String from,
                            @Lazy UserService userService) {
        this.mailSender = mailSender;
        this.from = from;
        this.userService = userService;
    }

    private record CodeEntry(String code, long expiresAt, CodeType type) {}

    @Override
    public void sendRegisterCode(String email) {
        if (email == null || email.isBlank())
            throw new BusinessException(400, "Email is required");
        if (userService.lambdaQuery().eq(User::getEmail, email).exists())
            throw new BusinessException("Email already registered");
        doSend(email, CodeType.REGISTER);
    }

    @Override
    public void sendResetCode(String email) {
        if (!userService.lambdaQuery().eq(User::getEmail, email).exists())
            throw new BusinessException(404, "Email not registered");
        doSend(email, CodeType.RESET_PASSWORD);
    }

    @Override
    public void sendChangeCode(String email) {
        if (email == null || email.isBlank())
            throw new BusinessException(400, "Email is required");
        if (userService.lambdaQuery().eq(User::getEmail, email).exists())
            throw new BusinessException("Email already registered");
        doSend(email, CodeType.CHANGE_EMAIL);
    }

    @Override
    public boolean verify(String email, String code, CodeType type) {
        if (email == null || code == null) return false;
        CodeEntry entry = codes.get(email);
        if (entry == null || System.currentTimeMillis() > entry.expiresAt) {
            codes.remove(email);
            return false;
        }
        if (entry.type != type) return false;
        if (entry.code.equals(code)) {
            codes.remove(email);
            return true;
        }
        return false;
    }

    private void doSend(String email, CodeType type) {
        CodeEntry existing = codes.get(email);
        if (existing != null && System.currentTimeMillis() < existing.expiresAt)
            throw new BusinessException(429, "Verification code already requested, please wait");
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(email, new CodeEntry(code, System.currentTimeMillis() + CODE_TTL, type));
        log.info("Verification code generated for {} (type={})", email, type);
        String subject = switch (type) {
            case REGISTER -> "Zincoid's - Registration Verification";
            case RESET_PASSWORD -> "Zincoid's - Password Reset Verification";
            case CHANGE_EMAIL -> "Zincoid's - Email Change Verification";
        };
        String purpose = switch (type) {
            case REGISTER -> "register your account";
            case RESET_PASSWORD -> "reset your password";
            case CHANGE_EMAIL -> "change your email";
        };
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(email);
                msg.setSubject(subject);
                msg.setText("""
                        Your verification code is: %s

                        Use this code to %s.
                        This code expires in 5 minutes.""".formatted(code, purpose));
                mailSender.send(msg);
                log.info("Verification code sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send verification code to {}", email, e);
                codes.remove(email);
            }
        });
    }
}
