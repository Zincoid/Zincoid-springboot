package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.RepoAccessType;
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
    private RepoAccessType type;
    private Access access;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
