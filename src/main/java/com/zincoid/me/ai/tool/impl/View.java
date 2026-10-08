package com.zincoid.me.ai.tool.impl;

import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolDef;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class View implements Tool {

    private record Args(String url) {}

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
            return ToolRes.of("Error: url must not be empty");
        if (url.startsWith("/"))
            url = siteUrl + url;
        if (!url.startsWith(siteUrl))
            return ToolRes.of("Error: only images hosted on this site can be viewed");
        log.info("Attach image: {}", url);
        return ToolRes.of("Image attached: %s".formatted(url), List.of(url));
    }
}
