package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.BizType;
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
    private NotificationType type;
    private Long relatedId;
    private BizType targetType;
    private Long targetId;
    private String snippet;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
