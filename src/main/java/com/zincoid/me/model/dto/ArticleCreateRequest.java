package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String contentMd;

    private String summary;
    private String coverImage;
    private Status status;
}
