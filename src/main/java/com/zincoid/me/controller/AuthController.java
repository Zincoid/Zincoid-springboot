package com.zincoid.me.controller;

import com.zincoid.me.model.dto.ForgotPasswordRequest;
import com.zincoid.me.model.dto.LoginRequest;
import com.zincoid.me.model.dto.RegisterRequest;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.LoginVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
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

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer "))
            userService.logout(header.substring(7));
        return ApiResponse.success();
    }

    @PostMapping("/register/send-code")
    public ApiResponse<Void> sendRegisterCode(@RequestBody Map<String, String> body) {
        emailService.sendRegisterCode(body.get("email"));
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
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
}
