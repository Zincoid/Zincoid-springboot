package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.AccessRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RepoAccessVO {

    private Long id;
    private Long repoId;
    private String repoName;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private AccessRole role;
    private Access access;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
