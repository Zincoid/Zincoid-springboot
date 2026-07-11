package com.zincoid.me.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepoItemAddRequest {

    @NotNull(message = "fileId is required")
    private Long fileId;

    @NotBlank(message = "Name is required")
    private String name;
}
