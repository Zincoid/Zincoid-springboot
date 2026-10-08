package com.zincoid.me.ai.tool.impl;

import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolDef;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.enums.Visibility;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class Search implements Tool {

    private record Args(String type, String username, String keyword) {}
    private record Hit(String kind, Long id, String title, Long userId,
                       String date, String text, String url) {}

    private static final int MAX_RESULTS = 5;
    private static final int SNIPPET_LENGTH = 160;
    private static final Set<String> TYPES = Set.of("moment", "article", "repo");

    private final MomentService momentService;
    private final ArticleService articleService;
    private final RepoService repoService;
    private final UserService userService;

    @Override
    public ToolDef def() {
        return ToolDef.builder("search", """
                        Search this website's own content: moments (short posts), articles and repositories.""")
                .addEnum("type", "Content type to search: moment, article or repo. Omit to search all types.", TYPES)
                .addString("username", "Only return content authored by this username. Omit to search all users.")
                .addString("keyword", "Search keyword.", true)
                .build();
    }

    @Override
    public ToolRes run(String json) {
        Args args = JsonUtil.parse(json, Args.class);
        if (args.keyword() == null || args.keyword().isBlank())
            return ToolRes.of("Error: keyword must not be empty");
        String keyword = args.keyword().trim();
        String type = args.type();
        if (type != null && !type.isBlank() && !TYPES.contains(type))
            return ToolRes.of("Error: unknown type \"%s\", valid values: moment, article, repo".formatted(type));
        Long userId = null;
        String username = args.username();
        if (username != null && !username.isBlank()) {
            User user = userService.lambdaQuery()
                    .eq(User::getUsername, username.trim()).one();
            if (user == null)
                return ToolRes.of("No user found with username \"%s\".".formatted(username.trim()));
            userId = user.getId();
        }
        List<Hit> hits = new ArrayList<>();
        if (type == null || type.isBlank() || "moment".equals(type))
            hits.addAll(searchMoments(keyword, userId));
        if (type == null || type.isBlank() || "article".equals(type))
            hits.addAll(searchArticles(keyword, userId));
        if (type == null || type.isBlank() || "repo".equals(type))
            hits.addAll(searchRepos(keyword, userId));
        if (hits.isEmpty())
            return ToolRes.of("No results found for \"%s\".".formatted(keyword));
        Map<Long, User> users = loadUsers(hits);
        StringBuilder sb = new StringBuilder("Search results for \"")
                .append(keyword).append("\":\n");
        for (Hit hit : hits) {
            sb.append("- [").append(hit.kind()).append(" id=").append(hit.id()).append("]");
            if (hit.title() != null)
                sb.append(" \"").append(hit.title()).append("\"");
            sb.append(" (by ").append(author(users.get(hit.userId())));
            if (hit.date() != null)
                sb.append(", ").append(hit.date());
            if (hit.url() != null && !hit.url().isBlank())
                sb.append(", ").append(hit.url());
            sb.append(")");
            if (hit.text() != null && !hit.text().isEmpty())
                sb.append(": ").append(hit.text());
            sb.append("\n");
        }
        return ToolRes.of(sb.toString());
    }

    // ──────── Private tool ────────────────────────────────

    private List<Hit> searchMoments(String keyword, Long userId) {
        List<Moment> rows = momentService.lambdaQuery()
                .eq(Moment::getStatus, Status.ACTIVE)
                .eq(Moment::getVisibility, Visibility.PUBLIC)
                .eq(userId != null, Moment::getUserId, userId)
                .like(Moment::getContent, keyword)
                .orderByDesc(Moment::getCreatedAt)
                .last("LIMIT %d".formatted(MAX_RESULTS))
                .list();
        return rows.stream()
                .map(m -> new Hit("moment", m.getId(), null, m.getUserId(),
                        date(m.getCreatedAt()), snippet(m.getContent()), null))
                .toList();
    }

    private List<Hit> searchArticles(String keyword, Long userId) {
        List<Article> rows = articleService.lambdaQuery()
                .eq(Article::getStatus, Status.ACTIVE)
                .eq(Article::getVisibility, Visibility.PUBLIC)
                .eq(userId != null, Article::getUserId, userId)
                .and(w -> w.like(Article::getTitle, keyword)
                        .or().like(Article::getSummary, keyword)
                        .or().like(Article::getContentMd, keyword))
                .orderByDesc(Article::getCreatedAt)
                .last("LIMIT %d".formatted(MAX_RESULTS))
                .list();
        return rows.stream()
                .map(a -> {
                    String text = a.getSummary() != null && !a.getSummary().isBlank()
                            ? a.getSummary() : a.getContentMd();
                    return new Hit("article", a.getId(), a.getTitle(), a.getUserId(),
                            date(a.getCreatedAt()), snippet(text), null);
                })
                .toList();
    }

    private List<Hit> searchRepos(String keyword, Long userId) {
        List<Repo> rows = repoService.lambdaQuery()
                .eq(Repo::getStatus, Status.ACTIVE)
                .ne(Repo::getVisibility, Visibility.PRIVATE)
                .eq(userId != null, Repo::getUserId, userId)
                .and(w -> w.like(Repo::getName, keyword)
                        .or().like(Repo::getDescription, keyword)
                        .or().like(Repo::getTags, keyword))
                .orderByDesc(Repo::getCreatedAt)
                .last("LIMIT %d".formatted(MAX_RESULTS))
                .list();
        return rows.stream()
                .map(r -> {
                    String tags = r.getTags() != null && !r.getTags().isBlank()
                            ? "tags: %s".formatted(r.getTags()) : null;
                    String text = snippet(r.getDescription());
                    if (tags != null)
                        text = text.isEmpty() ? tags : "%s; %s".formatted(tags, text);
                    return new Hit("repo", r.getId(), r.getName(), r.getUserId(),
                            date(r.getCreatedAt()), text, r.getUrl());
                })
                .toList();
    }

    private Map<Long, User> loadUsers(List<Hit> hits) {
        Set<Long> ids = hits.stream().map(Hit::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) return Map.of();
        return userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private String author(User user) {
        if (user == null) return "unknown";
        if (user.getNickname() != null && !user.getNickname().isBlank())
            return "%s@%s".formatted(user.getNickname(), user.getUsername());
        return user.getUsername();
    }

    private String date(java.time.LocalDateTime time) {
        return time != null ? time.toLocalDate().toString() : null;
    }

    private String snippet(String text) {
        if (text == null) return "";
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() > SNIPPET_LENGTH ? "%s...".formatted(t.substring(0, SNIPPET_LENGTH)) : t;
    }
}
