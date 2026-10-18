package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.type.BizType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentCardVO {

    private Long id;
    private Long userId;
    private String username;
    private String userNickname;
    private String userAvatar;
    private String content;
    private BizType targetType;
    private Long targetId;
    private Long parentUserId;
    private String parentUsername;
    private LocalDateTime createdAt;
}
