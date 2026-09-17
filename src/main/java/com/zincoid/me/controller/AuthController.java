package com.zincoid.me.controller;

import com.zincoid.me.model.dto.ForgotPasswordRequest;
import com.zincoid.me.model.dto.LoginRequest;
import com.zincoid.me.model.dto.RegisterRequest;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.LoginVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String COOKIE_NAME = "zincoid_token";

    private final UserService userService;
    private final EmailService emailService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${cookie.max-age:86400}")
    private long maxAge;

    // ──── Public endpoints ────────────────

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletResponse response) {
        LoginVO vo = userService.login(request);
        issueCookie(response, vo.getToken());
        return ApiResponse.success(vo);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies())
                if (COOKIE_NAME.equals(cookie.getName()))
                    token = cookie.getValue();
        }
        if (token != null) userService.logout(token);
        clearCookie(response);
        return ApiResponse.success();
    }

    @PostMapping("/register/send-code")
    public ApiResponse<Void> sendRegisterCode(@RequestBody Map<String, String> body) {
        emailService.sendRegisterCode(body.get("email"));
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request,
                                         HttpServletResponse response) {
        LoginVO vo = userService.register(request);
        issueCookie(response, vo.getToken());
        return ApiResponse.success(vo);
    }

    @PostMapping("/reset-password/send-code")
    public ApiResponse<Void> sendResetCode(@RequestBody Map<String, String> body) {
        emailService.sendResetCode(body.get("email"));
        return ApiResponse.success();
    }

    @PutMapping("/reset-password")
    public ApiResponse<Void> reset(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.resetPassword(request);
        return ApiResponse.success();
    }

    @PostMapping("/change-email/send-new-code")
    public ApiResponse<Void> sendChangeCode(@RequestBody Map<String, String> body) {
        emailService.sendChangeCode(body.get("email"));
        return ApiResponse.success();
    }

    @PostMapping("/change-email/send-old-code")
    public ApiResponse<Void> sendChangeCode() {
        emailService.sendChangeCode(AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @PutMapping("/change-email")
    public ApiResponse<Void> change(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newCode = body.get("newCode");
        String oldCode = body.get("oldCode");
        if (email == null || email.isBlank() || newCode == null || newCode.isBlank())
            return ApiResponse.badRequest("Email and newCode are required");
        userService.changeEmail(AuthCtx.getUserId(), email, newCode, oldCode);
        return ApiResponse.success();
    }

    // ──── Private tool ────────────────────

    private void issueCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
