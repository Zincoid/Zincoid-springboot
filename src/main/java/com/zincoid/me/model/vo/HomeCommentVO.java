package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.RelatedType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HomeCommentVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private RelatedType targetType;
    private Long targetId;
    private Long parentUserId;
    private String parentUsername;
    private LocalDateTime createdAt;
}
