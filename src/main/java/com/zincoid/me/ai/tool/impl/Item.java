package com.zincoid.me.ai.tool.impl;

import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolDef;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.enums.Visibility;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoItemVO;
import com.zincoid.me.service.RepoItemService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Item implements Tool {

    private record Args(Long id, Integer page) {}

    private static final int PAGE_SIZE = 10;

    private final RepoService repoService;
    private final RepoItemService repoItemService;

    @Value("${site.url}")
    private String siteUrl;

    @Override
    public ToolDef def() {
        return ToolDef.builder("item", """
                        List the items of a media (image) or file repository by repo id \
                        (as returned by search). Returns file names and URLs; \
                        images can be viewed with view or posted with send.""")
                .addInteger("id", "Repo id, as returned by search.", true)
                .addInteger("page", "Page number, 10 items per page. Omit for the first page.")
                .build();
    }

    @Override
    public ToolRes run(String json) {
        Args args = JsonUtil.parse(json, Args.class);
        if (args.id() == null)
            return ToolRes.error("id must not be empty");
        Long id = args.id();
        Repo repo = repoService.getById(id);
        if (repo == null || repo.getStatus() != Status.ACTIVE
                || repo.getVisibility() == Visibility.PRIVATE)
            return ToolRes.of("Not found: repo id=%d does not exist or is not publicly visible".formatted(id));
        if (repo.getVisibility() == Visibility.RESTRICTED)
            return ToolRes.error("repo id=%d is restricted, its items are not accessible".formatted(id));
        if (repo.getType() == RepoType.CODE)
            return ToolRes.error("code repositories have no items");
        int page = args.page() != null && args.page() > 0 ? args.page() : 1;
        PageVO<RepoItemVO> vo = repoItemService.list(id, page, PAGE_SIZE);
        if (vo.getTotal() == 0)
            return ToolRes.of("This repo has no items.");
        if (vo.getRecords().isEmpty())
            return ToolRes.of("No items on page %d.".formatted(page));

        StringBuilder sb = new StringBuilder(
                "Items of [repo id=%d] \"%s\" (%d total, page %d/%d):\n"
                        .formatted(id, repo.getName(), vo.getTotal(), vo.getPage(), vo.getPages()));
        for (RepoItemVO item : vo.getRecords()) {
            String url = item.getUrl() != null && !item.getUrl().startsWith("http")
                    ? siteUrl + item.getUrl() : item.getUrl();
            sb.append("- item id=%d: %s".formatted(item.getId(), item.getName()));
            if (item.getFileSize() != null)
                sb.append(" (%d bytes)".formatted(item.getFileSize()));
            if (url != null)
                sb.append(" %s".formatted(url));
            sb.append("\n");
        }
        if (repo.getType() == RepoType.MEDIA)
            sb.append("Use the view tool to look at the images, or the send tool to post them.\n");
        return ToolRes.of(sb.toString());
    }
}
