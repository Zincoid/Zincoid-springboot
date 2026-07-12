package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GitHubCommitVO {

    private String sha;
    private String message;
    private String author;
    private String authorAvatar;
    private LocalDateTime date;
}
