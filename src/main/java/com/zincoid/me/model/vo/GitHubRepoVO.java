package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GitHubRepoVO {

    private Integer stars;
    private Integer forks;
    private String language;
    private String description;
    private List<GitHubCommitVO> commits;
}
