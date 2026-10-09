package com.zincoid.me.ai.tool.impl;

import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolDef;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.service.FileService;
import com.zincoid.me.utils.FileUtil;
import com.zincoid.me.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class View implements Tool {

    private record Args(String url) {}

    private final FileService fileService;

    @Value("${site.url}")
    private String siteUrl;

    @Override
    public ToolDef def() {
        return ToolDef.builder("view", """
                        View an image hosted on this website. The image is attached to the conversation \
                        so you can see it. Only use when the visual content actually matters; \
                        prefer the text returned by other tools otherwise.""")
                .addString("url", "Image URL as listed by other tools.", true)
                .build();
    }

    @Override
    public ToolRes run(String json) {
        Args args = JsonUtil.parse(json, Args.class);
        String url = args.url() != null ? args.url().trim() : null;
        if (url == null || url.isBlank())
            return ToolRes.error("url must not be empty");
        if (url.startsWith("/"))
            url = siteUrl + url;
        if (!url.startsWith(siteUrl))
            return ToolRes.error("only images hosted on this site can be viewed");
        int q = url.indexOf('?');
        String clean = q >= 0 ? url.substring(0, q) : url;
        if (!FileUtil.isImage(FileUtil.getExt(clean)))
            return ToolRes.error("only images can be viewed; this file is not a supported image");
        if (!fileService.exists(clean.substring(siteUrl.length())))
            return ToolRes.error("image not found or no longer available");
        log.info("Attach image: {}", url);
        return ToolRes.of("Image attached: %s".formatted(url), List.of(url));
    }
}
