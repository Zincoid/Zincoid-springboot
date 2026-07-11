package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Visibility;
import lombok.Data;

import java.util.List;

@Data
public class RepoUpdateRequest {

    private String name;
    private String description;
    private String url;
    private List<String> tags;
    private String coverImage;
    private Visibility visibility;
}
