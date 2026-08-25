package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.RequestType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RequestVO {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private String receiverName;
    private String receiverAvatar;
    private RequestType type;
    private String meta;
    private Access access;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
