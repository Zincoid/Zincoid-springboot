package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Status;
import lombok.Data;

@Data
public class ArticleUpdateRequest {

    private String title;
    private String contentMd;
    private String summary;
    private String coverImage;
    private Status status;
}
