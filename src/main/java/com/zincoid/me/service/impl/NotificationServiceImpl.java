package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.NotificationMapper;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.Notification;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.NotificationVO;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final UserService userService;
    private final CommentService commentService;

    public NotificationServiceImpl(@Lazy UserService userService, @Lazy CommentService commentService) {
        this.userService = userService;
        this.commentService = commentService;
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
            if (n.getRelatedType() == RelatedType.COMMENT || n.getRelatedType() == RelatedType.REPLY) {
                Comment comment = commentService.getById(n.getRelatedId());
                if (comment == null) continue;
                String content = comment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = comment.getTargetType();
                targetId = comment.getTargetId();
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
    public void add(Long senderId, Long receiverId, RelatedType relatedType, Long relatedId) {
        if (senderId.equals(receiverId)) return;
        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        save(notification);
        log.info("Notification created: sender={}, receiver={}, relation={}:{}, id={}",
                senderId, receiverId, relatedType, relatedId, notification.getId());
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
    public void deleteAll(RelatedType relatedType, Long relatedId) {
        lambdaUpdate()
                .eq(Notification::getRelatedType, relatedType)
                .eq(Notification::getRelatedId, relatedId)
                .remove();
        log.info("Notifications deleted: relation={}:{}", relatedType, relatedId);
    }

    @Override
    public void deleteAll(Long userId) {
        lambdaUpdate().eq(Notification::getReceiverId, userId).remove();
        log.info("Notifications deleted: user={}", userId);
    }

    @Override
    public void readOne(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null || !notification.getReceiverId().equals(userId)) return;
        lambdaUpdate().eq(Notification::getId, notificationId).set(Notification::getIsRead, true).update();
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
