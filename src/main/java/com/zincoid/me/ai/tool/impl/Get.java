package com.zincoid.me.ai.tool.impl;

import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolDef;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.model.enums.RepoType;
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
import com.zincoid.me.utils.FileUtil;
import com.zincoid.me.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class Get implements Tool {

    private record Args(String type, Long id) {}

    private static final int MAX_CONTENT_LENGTH = 8192;
    private static final Set<String> TYPES = Set.of("moment", "article", "repo");

    private final MomentService momentService;
    private final ArticleService articleService;
    private final RepoService repoService;
    private final UserService userService;

    @Value("${site.url}")
    private String siteUrl;

    @Override
    public ToolDef def() {
        return ToolDef.builder("get", """
                        Get the full content of a moment, article or repository by type and id \
                        (as returned by search). Attached images are listed as URLs; \
                        use view to look at them selectively.""")
                .addEnum("type", "Content type: moment, article or repo.", TYPES, true)
                .addInteger("id", "Content id, as returned by search.", true)
                .build();
    }

    @Override
    public ToolRes run(String json) {
        Args args = JsonUtil.parse(json, Args.class);
        String type = args.type();
        if (type == null || !TYPES.contains(type))
            return ToolRes.error("unknown type \"%s\", valid values: moment, article, repo".formatted(type));
        if (args.id() == null)
            return ToolRes.error("id must not be empty");
        return switch (type) {
            case "moment" -> renderMoment(args.id());
            case "article" -> renderArticle(args.id());
            default -> renderRepo(args.id());
        };
    }

    // ──────── Private tool ────────────────────────────────

    private ToolRes renderMoment(Long id) {
        Moment m = momentService.getById(id);
        if (m == null || m.getStatus() != Status.ACTIVE || m.getVisibility() != Visibility.PUBLIC)
            return ToolRes.of("Not found: moment id=%d does not exist or is not publicly visible".formatted(id));
        StringBuilder sb = header("moment", m.getId(), null, m.getUserId(), m.getCreatedAt());
        sb.append(truncate(m.getContent())).append("\n");
        appendAttachments(sb, JsonUtil.parseImages(m.getUrls()));
        return ToolRes.of(sb.toString());
    }

    private ToolRes renderArticle(Long id) {
        Article a = articleService.getById(id);
        if (a == null || a.getStatus() != Status.ACTIVE || a.getVisibility() != Visibility.PUBLIC)
            return ToolRes.of("Not found: article id=%d does not exist or is not publicly visible".formatted(id));
        List<String> images = Collections.singletonList(a.getCoverImage());
        StringBuilder sb = header("article", a.getId(), a.getTitle(), a.getUserId(), a.getCreatedAt());
        if (a.getSummary() != null && !a.getSummary().isBlank())
            sb.append("summary: ").append(a.getSummary()).append("\n");
        sb.append(truncate(a.getContentMd())).append("\n");
        appendAttachments(sb, images);
        return ToolRes.of(sb.toString());
    }

    private ToolRes renderRepo(Long id) {
        Repo r = repoService.getById(id);
        if (r == null || r.getStatus() != Status.ACTIVE || r.getVisibility() == Visibility.PRIVATE)
            return ToolRes.of("Not found: repo id=%d does not exist or is not publicly visible".formatted(id));
        boolean restricted = r.getVisibility() == Visibility.RESTRICTED;
        StringBuilder sb = header("repo", r.getId(), r.getName(), r.getUserId(), r.getCreatedAt());
        RepoType repoType = r.getType();
        if (repoType != null)
            sb.append("type: ").append(repoType.name().toLowerCase()).append("\n");
        if (r.getTags() != null && !r.getTags().isBlank())
            sb.append("tags: ").append(r.getTags()).append("\n");
        if (!restricted && r.getUrl() != null && !r.getUrl().isBlank())
            sb.append("url: ").append(r.getUrl()).append("\n");
        if (r.getDescription() != null && !r.getDescription().isBlank())
            sb.append(truncate(r.getDescription())).append("\n");
        if (!restricted)
            appendAttachments(sb, Collections.singletonList(r.getCoverImage()));
        return ToolRes.of(sb.toString());
    }

    private StringBuilder header(String kind, Long id, String title, Long userId,
                                 java.time.LocalDateTime createdAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(kind).append(" id=").append(id).append("]");
        if (title != null)
            sb.append(" \"").append(title).append("\"");
        sb.append(" (by ").append(author(userService.getById(userId)));
        if (createdAt != null)
            sb.append(", ").append(createdAt.toLocalDate());
        sb.append("):\n");
        return sb;
    }

    private String author(User user) {
        if (user == null) return "unknown";
        if (user.getNickname() != null && !user.getNickname().isBlank())
            return "%s@%s".formatted(user.getNickname(), user.getUsername());
        return user.getUsername();
    }

    private void appendAttachments(StringBuilder sb, List<String> paths) {
        List<String> images = new ArrayList<>();
        List<String> others = new ArrayList<>();
        for (String path : paths) {
            if (path == null || path.isBlank()) continue;
            String url = path.startsWith("http") ? path : siteUrl + path;
            String ext = FileUtil.getExt(url);
            if (FileUtil.isVideo(ext) || FileUtil.isAudio(ext) || FileUtil.isDoc(ext))
                others.add(url);
            else
                images.add(url);
        }
        if (!images.isEmpty()) {
            sb.append("images: ").append(images).append("\n");
            sb.append("Use the view tool to look at any of these images.\n");
        }
        if (!others.isEmpty())
            sb.append("other attachments (not viewable): ").append(others).append("\n");
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > MAX_CONTENT_LENGTH
                ? "%s...(content truncated)".formatted(text.substring(0, MAX_CONTENT_LENGTH))
                : text;
    }
}
