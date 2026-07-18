package com.zincoid.me.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailBroadcastRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private Boolean force = false;
}
