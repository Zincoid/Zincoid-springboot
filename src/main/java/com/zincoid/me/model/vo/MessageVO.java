package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageVO {

    private Long id;
    private Long userId;
    private String username;
    private String userNickname;
    private String userAvatar;
    private String content;
    private String thumb;
    private String file;
    private LocalDateTime createdAt;
}
