package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Visibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RepoCardVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String name;
    private String description;
    private RepoType type;
    private Visibility visibility;
    private String url;
    private List<String> tags;
    private String coverThumb;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long itemCount;
    private Boolean isLiked;
    private Boolean restricted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
