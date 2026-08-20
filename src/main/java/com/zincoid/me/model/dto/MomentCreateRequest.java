package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Visibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MomentCreateRequest {

    private String content;
    @Size(max = 9, message = "urls must not exceed 9 items")
    private List<String> urls;
    private Visibility visibility;

    @AssertTrue(message = "content and urls must not both be empty")
    public boolean isValid() {
        return (content != null && !content.isBlank()) || (urls != null && !urls.isEmpty());
    }
}
