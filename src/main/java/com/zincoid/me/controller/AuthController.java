package com.zincoid.me.controller;

import com.zincoid.me.model.dto.ForgotPasswordRequest;
import com.zincoid.me.model.dto.LoginRequest;
import com.zincoid.me.model.dto.RegisterRequest;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.LoginVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;

    // ──── Public endpoints ────────────────

    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@RequestBody Map<String, String> body) {
        emailService.sendCode(body.get("email"));
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) userService.logout(header.substring(7));
        return ApiResponse.success();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.resetPasswordByEmail(request);
        return ApiResponse.success();
    }
}
