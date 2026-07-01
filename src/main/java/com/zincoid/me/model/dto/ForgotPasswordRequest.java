package com.zincoid.me.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Verification code is required")
    private String code;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;

    private String confirmPassword;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
