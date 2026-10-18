package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Permission;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserPermissionVO {

    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private Permission permission;
    private Long grantedBy;
    private String grantedByUsername;
    private LocalDateTime createdAt;
}
