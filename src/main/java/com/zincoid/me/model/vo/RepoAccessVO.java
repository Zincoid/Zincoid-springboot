package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.state.Approval;
import com.zincoid.me.model.enums.type.AccessType;
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
    private AccessType type;
    private Approval approval;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
