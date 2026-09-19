package com.zincoid.me.model.vo;

import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Visibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RepoDetailVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String name;
    private String description;
    private RepoType type;
    private Visibility visibility;
    private String url;
    private List<String> tags;
    private String coverImage;
    private Boolean isDefaultCover;
    private Long viewCount;
    private Long likeCount;
    private Long contributorCount;
    private Boolean isLiked;
    private List<LikerVO> recentLikers;
    private List<ContributorVO> recentContributors;
    private Boolean restricted;
    private Boolean contributed;
    private GitHubRepoVO github;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
