package com.zincoid.me.service.impl;

import com.zincoid.me.ai.AiChatProperties;
import com.zincoid.me.ai.client.ChatClient;
import com.zincoid.me.ai.message.AiMessage;
import com.zincoid.me.configuration.DataInitializer;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.AiService;
import com.zincoid.me.service.CommentService;
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
    private final CommentService commentService;
    private final ChatClient chatClient;
    private final AiChatProperties aiChatProperties;

    @Value("${site.url}")
    private String siteUrl;

    public AiServiceImpl(ConfigService configService,
                         UserService userService,
                         @Lazy MessageService messageService,
                         @Lazy CommentService commentService,
                         ChatClient chatClient,
                         AiChatProperties aiChatProperties) {
        this.configService = configService;
        this.userService = userService;
        this.messageService = messageService;
        this.commentService = commentService;
        this.chatClient = chatClient;
        this.aiChatProperties = aiChatProperties;
    }

    @Override
    public boolean isAi(Long userId) {
        User user = userService.getById(userId);
        return user != null && DataInitializer.AI_USERNAME.equals(user.getUsername());
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

    @Override
    @Async("AsyncRunner")
    public void comment(Long userId, Long commentId) {
        User ai = getAiUser();
        if (ai.getId().equals(userId)) return;
        Comment trigger = commentService.getById(commentId);
        if (trigger == null) return;
        String prompt = configService.get("ai_comment_prompt");
        String reply = chatClient.chat(buildComments(trigger), prompt, thinking);
        commentService.add(ai.getId(), trigger.getTargetType(), trigger.getTargetId(),
                reply, commentId);
    }

    // ──────── Private tool ────────────────────────────────

    private User getAiUser() {
        User ai = userService.lambdaQuery()
                .eq(User::getUsername, DataInitializer.AI_USERNAME).one();
        if (ai == null)
            throw new BusinessException(500, "AI user not initialized");
        return ai;
    }

    private List<AiMessage> buildComments(Comment trigger) {
        Long rootId = trigger.getRootId() != null ? trigger.getRootId() : trigger.getId();
        List<Comment> thread = commentService.lambdaQuery()
                .eq(Comment::getTargetType, trigger.getTargetType())
                .eq(Comment::getTargetId, trigger.getTargetId())
                .eq(Comment::getRootId, rootId)
                .orderByAsc(Comment::getCreatedAt)
                .list();
        Map<Long, Comment> byId = thread.stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));
        Map<Long, User> users = userService.listByIds(
                        thread.stream().map(Comment::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        StringBuilder sb = new StringBuilder();
        sb.append("You are mentioned in a comment on %s id=%d.\n".formatted(
                trigger.getTargetType().name().toLowerCase(), trigger.getTargetId()));
        sb.append("Comment thread:\n");
        for (Comment c : thread) {
            sb.repeat("  ", depth(byId, c));
            sb.append("[comment id=%d]".formatted(c.getId()));
            if (c.getParentId() != null)
                sb.append(" (reply to id=%d)".formatted(c.getParentId()));
            sb.append(" %s: %s\n".formatted(author(users.get(c.getUserId())), text(c.getContent())));
        }
        sb.append("The mention is in comment id=%d.".formatted(trigger.getId()));
        return List.of(AiMessage.user(sb.toString()));
    }

    private int depth(Map<Long, Comment> byId, Comment comment) {
        int depth = 0;
        Long parentId = comment.getParentId();
        while (parentId != null && depth < 5) {
            Comment parent = byId.get(parentId);
            if (parent == null) break;
            depth++;
            parentId = parent.getParentId();
        }
        return depth;
    }

    private String author(User user) {
        if (user == null) return "unknown";
        if (user.getNickname() != null && !user.getNickname().isBlank())
            return "%s@%s".formatted(user.getNickname(), user.getUsername());
        return user.getUsername();
    }

    private String text(String content) {
        if (content == null) return "";
        return content.length() > 2048 ? "%s...(truncated)".formatted(content.substring(0, 2048)) : content;
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
