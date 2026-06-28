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
    @Transactional
    public void add(Long senderId, Long receiverId, RelatedType relatedType, Long relatedId, Long commentId) {
        if (senderId.equals(receiverId)) return;
        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .commentId(commentId)
                .isRead(false)
                .build();
        save(notification);
        log.info("Notification created: sender={} receiver={} comment={}", senderId, receiverId, commentId);
    }

    @Override
    public void delete(Long userId) {
        lambdaUpdate().eq(Notification::getReceiverId, userId).remove();
        log.info("All notifications deleted for user {}", userId);
    }

    @Override
    public void deleteOne(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null)
            throw new BusinessException(404, "Notification not found");
        if (!notification.getReceiverId().equals(userId))
            throw new BusinessException(403, "No permission to delete this notification");
        removeById(notificationId);
    }

    @Override
    public long countUnread(Long userId) {
        return lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
                .count();
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
    public List<NotificationVO> list(Long userId) {
        List<Notification> notifications = lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .list();
        List<NotificationVO> vos = new ArrayList<>();
        for (Notification n : notifications) {
            User sender = userService.getById(n.getSenderId());
            Comment comment = commentService.getById(n.getCommentId());
            if (sender == null || comment == null) continue;
            String snippet = comment.getContent();
            if (snippet != null && snippet.length() > 80)
                snippet = snippet.substring(0, 80) + "...";
            vos.add(NotificationVO.builder()
                    .id(n.getId())
                    .senderId(sender.getId())
                    .senderNickname(sender.getNickname())
                    .senderAvatar(sender.getAvatar())
                    .relatedType(n.getRelatedType())
                    .relatedId(n.getRelatedId())
                    .commentId(n.getCommentId())
                    .commentSnippet(snippet)
                    .isReply(comment.getParentId() != null)
                    .isRead(n.getIsRead())
                    .createdAt(n.getCreatedAt())
                    .build());
        }
        return vos;
    }
}
