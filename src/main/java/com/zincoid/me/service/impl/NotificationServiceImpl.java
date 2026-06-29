package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.NotificationMapper;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.Notification;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.NotificationVO;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final UserService userService;
    private final CommentService commentService;
    private final MessageService messageService;
    private final MomentService momentService;

    public NotificationServiceImpl(@Lazy UserService userService, @Lazy CommentService commentService,
                                    @Lazy MessageService messageService, @Lazy MomentService momentService) {
        this.userService = userService;
        this.commentService = commentService;
        this.messageService = messageService;
        this.momentService = momentService;
    }

    @Override
    public List<NotificationVO> list(Long userId) {
        List<Notification> notifications = lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .list();
        List<NotificationVO> vos = new ArrayList<>();
        for (Notification n : notifications) {
            User sender = userService.getById(n.getSenderId());
            if (sender == null) continue;
            RelatedType targetType = null;
            Long targetId = null;
            String snippet = null;
            if (n.getRelatedType() == NotificationType.COMMENT || n.getRelatedType() == NotificationType.REPLY) {
                Comment comment = commentService.getById(n.getRelatedId());
                if (comment == null) continue;
                String content = comment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = comment.getTargetType();
                targetId = comment.getTargetId();
            } else if (n.getRelatedType() == NotificationType.MOMENT_MENTION) {
                Moment moment = momentService.getById(n.getRelatedId());
                if (moment == null) continue;
                String content = moment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = RelatedType.MOMENT;
                targetId = moment.getId();
            } else if (n.getRelatedType() == NotificationType.COMMENT_MENTION) {
                Comment comment = commentService.getById(n.getRelatedId());
                if (comment == null) continue;
                String content = comment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = comment.getTargetType();
                targetId = comment.getTargetId();
            } else if (n.getRelatedType() == NotificationType.CHAT_MENTION) {
                Message msg = messageService.getById(n.getRelatedId());
                if (msg == null) continue;
                String content = msg.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = RelatedType.CHAT;
            }
            vos.add(NotificationVO.builder()
                    .id(n.getId())
                    .senderId(sender.getId())
                    .senderNickname(sender.getNickname())
                    .senderAvatar(sender.getAvatar())
                    .relatedType(n.getRelatedType())
                    .relatedId(n.getRelatedId())
                    .targetType(targetType)
                    .targetId(targetId)
                    .snippet(snippet)
                    .isRead(n.getIsRead())
                    .createdAt(n.getCreatedAt())
                    .build());
        }
        return vos;
    }

    @Override
    @Transactional
    public void notify(Long senderId, Long receiverId, NotificationType type, Long relatedId) {
        if (senderId.equals(receiverId)) return;
        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .relatedType(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        save(notification);
        log.info("Notification created: sender={}, receiver={}, relation={}:{}, id={}",
                senderId, receiverId, type, relatedId, notification.getId());
    }

    @Override
    @Transactional
    public void notify(Long senderId, String content, NotificationType type, Long relatedId) {
        if (content == null) return;
        Matcher m = Pattern.compile("@(\\w{3,50})").matcher(content);
        Set<String> seen = new HashSet<>();
        while (m.find()) {
            String username = m.group(1);
            if (!seen.add(username)) continue;
            User mentioned = userService.lambdaQuery()
                    .eq(User::getUsername, username)
                    .eq(User::getStatus, Status.ACTIVE).one();
            if (mentioned != null && !mentioned.getId().equals(senderId))
                notify(senderId, mentioned.getId(), type, relatedId);
        }
    }

    @Override
    public void deleteOne(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null)
            throw new BusinessException(404, "Notification not found");
        if (!notification.getReceiverId().equals(userId))
            throw new BusinessException(403, "No permission to delete this notification");
        removeById(notificationId);
        log.info("Notification deleted: user={}, id={}", userId, notificationId);
    }

    @Override
    public void deleteAll(NotificationType type, Long relatedId) {
        lambdaUpdate()
                .eq(Notification::getRelatedType, type)
                .eq(Notification::getRelatedId, relatedId)
                .remove();
        log.info("Notifications deleted: relation={}:{}", type, relatedId);
    }

    @Override
    public void deleteAll(Long userId) {
        lambdaUpdate().eq(Notification::getReceiverId, userId).remove();
        log.info("Notifications deleted: user={}", userId);
    }

    @Override
    public void readOne(Long notificationId, Long userId) {
        lambdaUpdate()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getReceiverId, userId)
                .set(Notification::getIsRead, true)
                .update();
    }

    @Override
    public void readAll(Long userId) {
        lambdaUpdate()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .update();
    }

    @Override
    public long countUnread(Long userId) {
        return lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
                .count();
    }
}
