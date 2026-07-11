package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Visibility;
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
    private Visibility visibility;
}
