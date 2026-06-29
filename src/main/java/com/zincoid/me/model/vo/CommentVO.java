package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private Long parentId;
    private List<CommentVO> replies;
    private long replyCount;
    private LocalDateTime createdAt;
}
