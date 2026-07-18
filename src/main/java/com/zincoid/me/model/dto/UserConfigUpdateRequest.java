package com.zincoid.me.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserConfigUpdateRequest {

    @NotNull(message = "receiveEmail is required")
    private Boolean receiveEmail;
}
