package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ArticleCardVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String title;
    private String summary;
    private String coverImage;
    private Boolean isPinned;
    private Long viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
}
