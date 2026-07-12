package com.zincoid.me.service.impl;

import com.zincoid.me.model.vo.GitHubCommitVO;
import com.zincoid.me.model.vo.GitHubRepoVO;
import com.zincoid.me.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GitHubServiceImpl implements GitHubService {

    private static final int MAX_COMMITS = 10;
    private static final Pattern GITHUB_URL = Pattern.compile("github\\.com/([^/]+)/([^/]+)");

    @Value("${github.token:}")
    private String token;

    @Override
    public GitHubRepoVO fetch(String url) {
        if (url == null || url.isBlank()) return null;
        Matcher m = GITHUB_URL.matcher(url);
        if (!m.find()) return null;
        String owner = m.group(1);
        String repo = m.group(2).replaceAll("\\.git$", "");
        try {
            GitHubBuilder builder = new GitHubBuilder();
            if (token != null && !token.isBlank())
                builder.withOAuthToken(token);
            GitHub github = builder.build();
            GHRepository ghRepo = github.getRepository(owner + "/" + repo);
            return GitHubRepoVO.builder()
                    .stars(ghRepo.getStargazersCount())
                    .forks(ghRepo.getForksCount())
                    .language(ghRepo.getLanguage())
                    .description(ghRepo.getDescription())
                    .commits(fetchCommits(ghRepo))
                    .build();
        } catch (IOException e) {
            log.warn("Failed to fetch GitHub repo: {}/{}", owner, repo);
            return null;
        }
    }

    // ──────── Private tool ────────────────────────────────

    private List<GitHubCommitVO> fetchCommits(GHRepository ghRepo) {
        List<GitHubCommitVO> commits = new ArrayList<>();
        try {
            for (GHCommit c : ghRepo.listCommits()) {
                if (commits.size() >= MAX_COMMITS) break;
                commits.add(GitHubCommitVO.builder()
                        .sha(c.getSHA1().substring(0, 7))
                        .message(c.getCommitShortInfo().getMessage())
                        .author(c.getCommitShortInfo().getAuthor().getName())
                        .authorAvatar(c.getAuthor() != null ? c.getAuthor().getAvatarUrl() : null)
                        .date(c.getCommitShortInfo().getAuthor().getDate() != null
                                ? c.getCommitShortInfo().getAuthor().getDate()
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                                : null)
                        .build());
            }
        } catch (IOException e) {
            log.warn("Failed to fetch commits", e);
        }
        return commits;
    }
}
