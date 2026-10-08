package com.zincoid.me.ai.tool.impl;

import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolDef;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.configuration.DataInitializer;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.FileUtil;
import com.zincoid.me.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Send implements Tool {

    private record Args(String content, String image) {}

    private final MessageService messageService;
    private final UserService userService;

    @Value("${site.url}")
    private String siteUrl;

    public Send(@Lazy MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @Override
    public ToolDef def() {
        return ToolDef.builder("send", """
                        Send a message to the chat room as the AI assistant right away. \
                        This is the only way to post images into the room; the message can also carry text. \
                        One image per call — call again to send more. \
                        Use it for intermediate output before your final answer.""")
                .addString("content", "Message text. Optional if image is given.")
                .addString("image", "Image URL as listed by other tools. Optional if content is given.")
                .build();
    }

    @Override
    public ToolRes run(String json) {
        Args args = JsonUtil.parse(json, Args.class);
        String content = args.content() != null && !args.content().isBlank()
                ? args.content().trim() : null;
        String image = args.image() != null && !args.image().isBlank()
                ? args.image().trim() : null;
        String file = null;
        if (image != null) {
            String url = image.startsWith("/") ? siteUrl + image : image;
            if (!url.startsWith(siteUrl))
                return ToolRes.of("Error: only images hosted on this site can be sent");
            file = url.substring(siteUrl.length());
            if (!FileUtil.isImage(FileUtil.getExt(file)))
                return ToolRes.of("Error: not an image file: %s".formatted(image));
        }
        if (content == null && file == null)
            return ToolRes.of("Error: content or image is required");
        User ai = userService.lambdaQuery()
                .eq(User::getUsername, DataInitializer.AI_USERNAME).one();
        if (ai == null)
            return ToolRes.of("Error: AI user not initialized");
        MessageVO vo = messageService.send(ai.getId(), content, file);
        return ToolRes.of("Sent message id=%d.".formatted(vo.getId()));
    }
}
