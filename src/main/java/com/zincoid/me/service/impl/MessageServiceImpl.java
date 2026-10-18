package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.module.core.DataInitializer;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.Conf;
import com.zincoid.me.model.Error;
import com.zincoid.me.mapper.MessageMapper;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.AiService;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.converter.MessageConverter;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.MessageStreamService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final ConfigService configService;
    private final FileService fileService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AiService aiService;
    private final MessageStreamService messageStreamService;

    @Override
    @Transactional
    public MessageVO send(Long userId, String content, String file, Long replyId) {
        if ((content == null || content.isBlank()) && (file == null || file.isBlank()))
            throw new BusinessException(Error.CONTENT_OR_FILE_REQUIRED);
        Message reply = null;
        if (replyId != null) {
            reply = getById(replyId);
            if (reply == null)
                throw new BusinessException(Error.MESSAGE_NOT_FOUND);
        }
        Message msg = Message.builder()
                .userId(userId)
                .replyId(replyId)
                .content(content != null && !content.isBlank() ? content : null)
                .file(file != null && !file.isBlank() ? file : null)
                .createdAt(LocalDateTime.now())  // can't fill back, set manually
                .build();
        save(msg);
        if (msg.getFile() != null && !aiService.isAi(userId))  // AI ignore link
            fileService.link(List.of(msg.getFile()), RelatedType.CHAT, msg.getId(), userId);
        List<Long> trimmed = trim();
        boolean isAiMentioned = false;
        for (String name : StrUtil.extractAts(msg.getContent())) {
            if (DataInitializer.AI_USERNAME.equals(name)) { isAiMentioned = true; continue; }
            notificationService.notifyAt(userId, name, NotificationType.CHAT_MENTION, msg.getId());
        }
        if (isAiMentioned)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiService.chat(userId, msg.getId());
                }
            });
        log.info("Message sent: user={}, id={}", userId, msg.getId());
        MessageVO vo = buildVO(msg, reply);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                trimmed.forEach(messageStreamService::trim);
                messageStreamService.broadcast(vo);
            }
        });
        return vo;
    }

    @Override
    @Transactional
    public MessageVO send(Long userId, String content, String file) {
        return send(userId, content, file, null);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long messageId, boolean isAdmin) {
        Message msg = getById(messageId);
        if (msg == null) throw new BusinessException(Error.MESSAGE_NOT_FOUND);
        if (!isAdmin && !msg.getUserId().equals(userId))
            throw new BusinessException(Error.ONLY_DELETE_OWN_MESSAGES);
        if (msg.getFile() != null)
            fileService.delete(RelatedType.CHAT, msg.getId());
        removeById(msg.getId());
        notificationService.deleteAll(NotificationType.CHAT_MENTION, msg.getId());
        log.info("Message deleted: user={}, id={}", msg.getUserId(), messageId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageStreamService.delete(messageId);
            }
        });
    }

    @Override
    public PageVO<MessageVO> list(int page, int size) {
        Page<Message> msgPage = lambdaQuery()
                .orderByAsc(Message::getCreatedAt)
                .page(Page.of(page, size));
        Map<Long, Message> replies = getReplies(msgPage.getRecords());
        return PageVO.of(msgPage, m -> buildVO(m, m.getReplyId() != null
                ? replies.get(m.getReplyId()) : null));
    }

    // ──────── Private tool ────────────────────────────────

    private List<Long> trim() {
        int max;
        try {
            max = Integer.parseInt(configService.get(Conf.MESSAGE_MAX_COUNT));
        } catch (NumberFormatException e) {
            max = Integer.parseInt(Conf.MESSAGE_MAX_COUNT.getDefaultValue());
        }
        long total = count();
        if (total <= max) return List.of();
        int toDelete = (int) (total - max);
        List<Message> oldest = lambdaQuery()
                .orderByAsc(Message::getCreatedAt)
                .last("LIMIT " + toDelete)
                .list();
        List<Long> deleted = new ArrayList<>();
        for (Message m : oldest) {
            if (m.getFile() != null)
                fileService.delete(RelatedType.CHAT, m.getId());
            notificationService.deleteAll(NotificationType.CHAT_MENTION, m.getId());
            removeById(m.getId());
            deleted.add(m.getId());
        }
        return deleted;
    }

    private Map<Long, Message> getReplies(List<Message> messages) {
        Set<Long> ids = messages.stream()
                .map(Message::getReplyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return listByIds(ids).stream().collect(Collectors.toMap(Message::getId, m -> m));
    }

    private MessageVO buildVO(Message msg, Message reply) {
        User user = userService.getById(msg.getUserId());
        MessageVO vo = MessageConverter.INSTANCE.toVO(msg, user);
        if (reply != null) vo.setReply(buildVO(reply));
        return vo;
    }

    private MessageVO buildVO(Message msg) {
        User user = userService.getById(msg.getUserId());
        return MessageConverter.INSTANCE.toVO(msg, user);
    }
}
