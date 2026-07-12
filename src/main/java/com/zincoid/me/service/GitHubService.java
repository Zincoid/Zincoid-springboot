package com.zincoid.me.service;

import com.zincoid.me.model.vo.GitHubRepoVO;

public interface GitHubService {

    GitHubRepoVO fetch(String url);
}
