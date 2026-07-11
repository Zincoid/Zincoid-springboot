package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RepoCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Type is required")
    private RepoType type;

    private Visibility visibility;
    private String url;
    private List<String> tags;
    private String coverImage;
}
