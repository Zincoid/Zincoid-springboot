package com.zincoid.me.service.impl;

import com.zincoid.me.ai.AiChatProperties;
import com.zincoid.me.ai.client.ChatClient;
import com.zincoid.me.ai.message.AiMessage;
import com.zincoid.me.configuration.DataInitializer;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.AiService;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final boolean thinking = true;

    private final ConfigService configService;
    private final UserService userService;
    private final MessageService messageService;
    private final ChatClient chatClient;
    private final AiChatProperties aiChatProperties;

    @Value("${site.url}")
    private String siteUrl;

    public AiServiceImpl(ConfigService configService,
                         UserService userService,
                         @Lazy MessageService messageService,
                         ChatClient chatClient,
                         AiChatProperties aiChatProperties) {
        this.configService = configService;
        this.userService = userService;
        this.messageService = messageService;
        this.chatClient = chatClient;
        this.aiChatProperties = aiChatProperties;
    }

    @Override
    @Async("AsyncRunner")
    public void chat(Long userId) {
        User ai = getAiUser();
        if (ai.getId().equals(userId)) return;
        List<AiMessage> history = buildHistory(ai.getId());
        String prompt = configService.get("ai_chat_prompt");
        String reply = chatClient.chat(history, prompt, thinking);
        messageService.send(ai.getId(), reply, null);
    }

    // ──────── Private tool ────────────────────────────────

    private User getAiUser() {
        User ai = userService.lambdaQuery()
                .eq(User::getUsername, DataInitializer.AI_USERNAME).one();
        if (ai == null)
            throw new BusinessException(500, "AI user not initialized");
        return ai;
    }

    private List<AiMessage> buildHistory(Long aiId) {
        List<Message> rows = messageService.lambdaQuery()
                .orderByDesc(Message::getId)
                .last("LIMIT " + aiChatProperties.getMaxLength())
                .list();
        Collections.reverse(rows);
        Map<Long, User> users = userService.listByIds(
                        rows.stream().map(Message::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        List<AiMessage> messages = new ArrayList<>();
        for (Message m : rows) {
            if (aiId.equals(m.getUserId())) {
                messages.add(AiMessage.assistant(m.getContent() != null ? m.getContent() : ""));
                continue;
            }
            List<String> images = buildImageUrls(m.getFile());
            String text = m.getContent() != null ? m.getContent() : "";
            if (text.isBlank() && images.isEmpty()) continue;
            User user = users.get(m.getUserId());
            String name = user != null ? user.getUsername() : null;
            String label = name;
            if (user != null && user.getNickname() != null && !user.getNickname().isBlank())
                label = user.getNickname() + "@" + name;
            messages.add(AiMessage.user(name, label != null ? label + ": " + text : text, images));
        }
        return messages;
    }

    private List<String> buildImageUrls(String file) {
        if (!FileUtil.isImage(FileUtil.getExt(file))) return List.of();
        return List.of(siteUrl + file);
    }
}
