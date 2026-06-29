package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ArticleDetailVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String title;
    private String contentMd;
    private String contentHtml;
    private String summary;
    private String coverImage;
    private Boolean isPinned;
    private Long viewCount;
    private Integer likeCount;
    private Boolean isLiked;
    private List<LikerVO> recentLikers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
