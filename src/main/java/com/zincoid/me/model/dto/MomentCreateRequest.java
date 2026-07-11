package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Visibility;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.util.List;

@Data
public class MomentCreateRequest {

    private String content;
    private List<String> images;
    private Visibility visibility;

    @AssertTrue(message = "content and images must not both be empty")
    public boolean isValid() {
        return (content != null && !content.isBlank()) || (images != null && !images.isEmpty());
    }
}
