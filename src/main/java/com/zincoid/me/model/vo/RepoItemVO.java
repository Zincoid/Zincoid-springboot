package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RepoItemVO {

    private Long id;
    private Integer sortOrder;
    private Long fileId;
    private String name;
    private String url;
    private Long fileSize;
    private LocalDateTime createdAt;
}
