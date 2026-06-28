package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.RelatedType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationVO {

    private Long id;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private RelatedType relatedType;
    private Long relatedId;
    private Long commentId;
    private String commentSnippet;
    private Boolean isReply;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
