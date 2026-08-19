package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Visibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MomentCardVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private List<String> urls;
    private List<String> thumbs;
    private Boolean isPinned;
    private Visibility visibility;
    private Integer commentCount;
    private Integer likeCount;
    private Long viewCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
}
