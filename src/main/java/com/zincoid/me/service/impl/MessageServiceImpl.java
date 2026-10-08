package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.ai.AiChatProperties;
import com.zincoid.me.ai.client.ChatClient;
import com.zincoid.me.ai.message.AiMessage;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.MessageMapper;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.converter.MessageConverter;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private static final ReentrantLock AI_CHAT_LOCK = new ReentrantLock();

    private final ConfigService configService;
    private final FileService fileService;
    private final UserService userService;
    private final NotificationService notificationService;

    private final ChatClient chatClient;
    private final AiChatProperties aiChatProperties;

    @Value("${site.url}")
    private String siteUrl;

    @Override
    @Transactional
    public MessageVO send(Long userId, String content, String file) {
        if ((content == null || content.isBlank()) && (file == null || file.isBlank()))
            throw new BusinessException(400, "Content or file is required");
        Message msg = Message.builder()
                .userId(userId)
                .content(content != null && !content.isBlank() ? content : null)
                .file(file != null && !file.isBlank() ? file : null)
                .createdAt(LocalDateTime.now())  // 无法回填需手动设置
                .build();
        save(msg);
        if (msg.getFile() != null)
            fileService.link(List.of(msg.getFile()), RelatedType.CHAT, msg.getId(), userId);
        trim();
        notificationService.notifyAt(userId, msg.getContent(), NotificationType.CHAT_MENTION, msg.getId());
        log.info("Message sent: user={}, id={}", userId, msg.getId());
        return buildVO(msg);
    }

    @Override
    @Transactional
    public MessageVO sendToAi(Long userId, String content, String file, boolean thinking) {
        AI_CHAT_LOCK.lock();
        try {
            send(userId, content, file);
            User ai = aiUser();
            List<AiMessage> history = buildAiHistory(ai.getId());
            String prompt = configService.get("ai_chat_prompt");
            String reply = chatClient.chat(history, prompt, thinking);
            return send(ai.getId(), reply, null);
        } finally {
            AI_CHAT_LOCK.unlock();
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, Long messageId, boolean isAdmin) {
        Message msg = getById(messageId);
        if (msg == null) throw new BusinessException(404, "Message not found");
        if (!isAdmin && !msg.getUserId().equals(userId))
            throw new BusinessException(403, "You can only delete your own messages");
        if (msg.getFile() != null)
            fileService.delete(RelatedType.CHAT, msg.getId());
        removeById(msg.getId());
        notificationService.deleteAll(NotificationType.CHAT_MENTION, msg.getId());
        log.info("Message deleted: user={}, id={}", msg.getUserId(), messageId);
    }

    @Override
    public PageVO<MessageVO> list(int page, int size) {
        Page<Message> msgPage = lambdaQuery()
                .orderByAsc(Message::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(msgPage, this::buildVO);
    }

    // ──────── Private tool ────────────────────────────────

    private User aiUser() {
        User ai = userService.lambdaQuery().eq(User::getUsername, "bot").one();
        if (ai == null)
            throw new BusinessException(500, "AI user not initialized");
        return ai;
    }

    private List<AiMessage> buildAiHistory(Long aiId) {
        List<Message> rows = lambdaQuery()
                .orderByDesc(Message::getId)
                .last("LIMIT " + aiChatProperties.getMaxLength())
                .list();
        Collections.reverse(rows);
        List<AiMessage> messages = new ArrayList<>();
        for (Message m : rows) {
            if (aiId.equals(m.getUserId())) {
                messages.add(AiMessage.assistant(m.getContent() != null ? m.getContent() : ""));
                continue;
            }
            List<String> images = imageUrls(m.getFile());
            String text = m.getContent() != null ? m.getContent() : "";
            if (text.isBlank() && images.isEmpty()) continue;
            messages.add(AiMessage.user(text, images));
        }
        return messages;
    }

    private List<String> imageUrls(String file) {
        if (!FileUtil.isImage(FileUtil.getExt(file))) return List.of();
        return List.of(siteUrl + file);
    }

    private void trim() {
        String maxStr = configService.get("message_max_count");
        int max = 200;
        try { if (maxStr != null) max = Integer.parseInt(maxStr); }
        catch (NumberFormatException ignored) {}
        long total = count();
        if (total <= max) return;
        int toDelete = (int) (total - max);
        List<Message> oldest = lambdaQuery()
                .orderByAsc(Message::getCreatedAt)
                .last("LIMIT " + toDelete)
                .list();
        for (Message m : oldest) {
            if (m.getFile() != null)
                fileService.delete(RelatedType.CHAT, m.getId());
            notificationService.deleteAll(NotificationType.CHAT_MENTION, m.getId());
            removeById(m.getId());
        }
    }

    private MessageVO buildVO(Message msg) {
        User user = userService.getById(msg.getUserId());
        return MessageConverter.INSTANCE.toVO(msg, user);
    }
}
