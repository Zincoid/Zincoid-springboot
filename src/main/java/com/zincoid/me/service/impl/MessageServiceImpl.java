package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final UserService userService;
    private final FileService fileService;
    private final ConfigService configService;
    private final NotificationService notificationService;

    public MessageServiceImpl(@Lazy UserService userService, FileService fileService,
                               ConfigService configService, NotificationService notificationService) {
        this.userService = userService;
        this.fileService = fileService;
        this.configService = configService;
        this.notificationService = notificationService;
    }

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
            fileService.link(List.of(msg.getFile()), RelatedType.CHAT, msg.getId());
        trim();
        notificationService.notify(userId, msg.getContent(), NotificationType.CHAT_MENTION, msg.getId());
        log.info("Message sent: user={}, id={}", userId, msg.getId());
        return buildVO(msg);
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
            removeById(m.getId());
        }
    }

    private MessageVO buildVO(Message msg) {
        User user = userService.getById(msg.getUserId());
        return MessageVO.builder()
                .id(msg.getId())
                .userId(msg.getUserId())
                .userNickname(user != null ? user.getNickname() : null)
                .userAvatar(user != null ? user.getAvatar() : null)
                .content(msg.getContent())
                .file(msg.getFile())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
