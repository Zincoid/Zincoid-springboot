package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MomentDetailVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private List<String> images;
    private Boolean isPinned;
    private List<CommentVO> comments;
    private Integer likeCount;
    private Long viewCount;
    private Boolean isLiked;
    private List<LikerVO> recentLikers;
    private LocalDateTime createdAt;
}
