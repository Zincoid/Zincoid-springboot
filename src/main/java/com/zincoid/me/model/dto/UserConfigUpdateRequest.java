package com.zincoid.me.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserConfigUpdateRequest {

    @NotNull(message = "receiveEmail is required")
    private Boolean receiveEmail;
    @NotNull(message = "receiveEmailSys is required")
    private Boolean receiveEmailSys;
    @NotNull(message = "receiveEmailRepoAccess is required")
    private Boolean receiveEmailRepoAccess;
}
