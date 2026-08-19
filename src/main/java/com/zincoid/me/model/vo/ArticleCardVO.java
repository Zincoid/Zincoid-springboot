package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Visibility;
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
    private String coverThumb;
    private Boolean isPinned;
    private Visibility visibility;
    private Long viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
}
