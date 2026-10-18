package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.dict.Error;
import com.zincoid.me.mapper.NotificationMapper;
import com.zincoid.me.model.enums.state.Role;
import com.zincoid.me.model.enums.state.Status;
import com.zincoid.me.model.enums.type.BizType;
import com.zincoid.me.model.enums.type.NotificationType;
import com.zincoid.me.model.po.*;
import com.zincoid.me.model.vo.NotificationVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.converter.NotificationConverter;
import com.zincoid.me.service.*;
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
    private final MomentService momentService;
    private final ArticleService articleService;
    private final RepoService repoService;
    private final MessageService messageService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final RequestService requestService;


    public NotificationServiceImpl(UserService userService,
                                   @Lazy MomentService momentService,
                                   @Lazy ArticleService articleService,
                                   @Lazy RepoService repoService,
                                   @Lazy MessageService messageService,
                                   @Lazy LikeService likeService,
                                   @Lazy CommentService commentService,
                                   @Lazy RequestService requestService) {
        this.userService = userService;
        this.momentService = momentService;
        this.articleService = articleService;
        this.repoService = repoService;
        this.messageService = messageService;
        this.likeService = likeService;
        this.commentService = commentService;
        this.requestService = requestService;
    }

    @Override
    public PageVO<NotificationVO> list(Long userId, int page, int size) {
        Page<Notification> notificationPage = lambdaQuery()
                .eq(Notification::getReceiverId, userId)
                .orderByAsc(Notification::getIsRead)
                .orderByDesc(Notification::getCreatedAt)
                .page(Page.of(page, size));
        List<NotificationVO> vos = new ArrayList<>();
        for (Notification n : notificationPage.getRecords()) {
            User sender = userService.getById(n.getSenderId());
            if (sender == null) continue;
            BizType targetType = null;
            Long targetId = null;
            String snippet = null;
            if (n.getType() == NotificationType.COMMENT || n.getType() == NotificationType.REPLY) {
                Comment comment = commentService.getById(n.getRelatedId());
                if (comment == null) continue;
                String content = comment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = comment.getTargetType();
                targetId = comment.getTargetId();
            } else if (n.getType() == NotificationType.MOMENT_MENTION) {
                Moment moment = momentService.getById(n.getRelatedId());
                if (moment == null) continue;
                String content = moment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = BizType.MOMENT;
                targetId = moment.getId();
            } else if (n.getType() == NotificationType.COMMENT_MENTION) {
                Comment comment = commentService.getById(n.getRelatedId());
                if (comment == null) continue;
                String content = comment.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = comment.getTargetType();
                targetId = comment.getTargetId();
            } else if (n.getType() == NotificationType.CHAT_MENTION) {
                Message msg = messageService.getById(n.getRelatedId());
                if (msg == null) continue;
                String content = msg.getContent();
                if (content != null && content.length() > 80)
                    content = content.substring(0, 80) + "...";
                snippet = content;
                targetType = BizType.CHAT;
            } else if (n.getType() == NotificationType.LIKE) {
                Like like = likeService.getById(n.getRelatedId());
                if (like == null) continue;
                targetType = like.getTargetType();
                targetId = like.getTargetId();
                if (targetType == BizType.MOMENT) {
                    Moment moment = momentService.lambdaQuery().select(Moment::getContent).eq(Moment::getId, targetId).one();
                    if (moment != null) snippet = moment.getContent();
                } else if (targetType == BizType.ARTICLE) {
                    Article article = articleService.lambdaQuery().select(Article::getTitle).eq(Article::getId, targetId).one();
                    if (article != null) snippet = article.getTitle();
                } else if (targetType == BizType.REPO) {
                    Repo repo = repoService.lambdaQuery().select(Repo::getName).eq(Repo::getId, targetId).one();
                    if (repo != null) snippet = repo.getName();
                } else if (targetType == BizType.COMMENT) {
                    Comment comment = commentService.getById(targetId);
                    if (comment != null) snippet = comment.getContent();
                }
                if (snippet != null && snippet.length() > 80)
                    snippet = snippet.substring(0, 80) + "...";
            } else if (n.getType() == NotificationType.SYSTEM) {
                snippet = n.getMessage();
            } else if (n.getType() == NotificationType.REQUEST) {
                Request request = requestService.getById(n.getRelatedId());
                snippet = """
                        Detail: type=%s, meta=%s
                        Status: %s""".formatted(request.getType(),
                        request.getMeta(), n.getMessage());
            } else if (n.getType() == NotificationType.REGISTER) {
                snippet = sender.getEmail();
            } else if (n.getType() == NotificationType.REPO_VIEWER_ACCESS_PENDING
                    || n.getType() == NotificationType.REPO_VIEWER_ACCESS_REJECTED
                    || n.getType() == NotificationType.REPO_VIEWER_ACCESS_APPROVED
                    || n.getType() == NotificationType.REPO_CONTRIBUTOR_ACCESS_PENDING
                    || n.getType() == NotificationType.REPO_CONTRIBUTOR_ACCESS_REJECTED
                    || n.getType() == NotificationType.REPO_CONTRIBUTOR_ACCESS_APPROVED) {
                Repo repo = repoService.getById(n.getRelatedId());
                snippet = repo.getName();
            }
            vos.add(NotificationConverter.INSTANCE.toVO(n, sender, targetType, targetId, snippet));
        }
        return PageVO.of(notificationPage, vos);
    }

    @Override
    @Transactional
    public void notify(Long senderId, Long receiverId, NotificationType type, Long relatedId) {
        if (senderId.equals(receiverId))
            throw new BusinessException(Error.SENDER_RECEIVER_SAME);
        if (userService.getById(receiverId) == null)
            throw new BusinessException(Error.USER_NOT_FOUND);
        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        save(notification);
        log.info("Notification created: sender={}, receiver={}, relation={}:{}, id={}",
                senderId, receiverId, type, relatedId, notification.getId());
    }

    @Override
    @Transactional
    public void notifyAt(Long senderId, String receiverUsername, NotificationType type, Long relatedId) {
        User receiver = userService.lambdaQuery()
                .eq(User::getUsername, receiverUsername)
                .eq(User::getStatus, Status.ACTIVE).one();
        if (receiver != null && !receiver.getId().equals(senderId))
            notify(senderId, receiver.getId(), type, relatedId);
    }

    @Override
    @Transactional
    public void notifyReq(Long senderId, Long receiverId, String message, Long requestId, boolean isAdminOnly) {
        if (senderId.equals(receiverId))
            throw new BusinessException(Error.SENDER_RECEIVER_SAME);
        if (!isAdminOnly) {
            if (userService.getById(receiverId) == null)
                throw new BusinessException(Error.USER_NOT_FOUND);
            Notification notification = Notification.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .type(NotificationType.REQUEST)
                    .relatedId(requestId)
                    .message(message)
                    .isRead(false)
                    .build();
            save(notification);
            log.info("Req notification created: sender={}, receiver={}, relation={}:{}, id={}",
                    senderId, receiverId, NotificationType.REQUEST, requestId, notification.getId());
            return;
        }
        List<User> admins = userService.lambdaQuery()
                .eq(User::getRole, Role.ADMIN)
                .eq(User::getStatus, Status.ACTIVE)
                .list();
        List<Notification> batch = new ArrayList<>();
        for (User admin : admins) {
            if (admin.getId().equals(senderId)) continue;
            batch.add(Notification.builder()
                    .senderId(senderId)
                    .receiverId(admin.getId())
                    .type(NotificationType.REQUEST)
                    .relatedId(requestId)
                    .message(message)
                    .isRead(false)
                    .build());
        }
        if (!batch.isEmpty()) saveBatch(batch);
        log.info("Req notification created (admin only): sender={}, recipients={}", senderId, batch.size());
    }

    @Override
    @Transactional
    public void broadcast(Long senderId, String message) {
        if (message == null || message.isBlank())
            throw new BusinessException(Error.CONTENT_REQUIRED);
        List<User> users = userService.lambdaQuery()
                .eq(User::getStatus, Status.ACTIVE)
                .list();
        List<Notification> batch = new ArrayList<>();
        for (User user : users) {
            if (user.getId().equals(senderId)) continue;
            Notification notification = Notification.builder()
                    .senderId(senderId)
                    .receiverId(user.getId())
                    .type(NotificationType.SYSTEM)
                    .relatedId(-1L)
                    .message(message.trim())
                    .isRead(false)
                    .build();
            batch.add(notification);
        }
        if (!batch.isEmpty()) {
            saveBatch(batch);
        }
        log.info("Broadcast notification created: sender={}, recipients={}", senderId, batch.size());
    }

    @Override
    public void deleteOne(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null)
            throw new BusinessException(Error.NOTIFICATION_NOT_FOUND);
        if (!notification.getReceiverId().equals(userId))
            throw new BusinessException(Error.NO_PERMISSION_DELETE_NOTIFICATION);
        removeById(notificationId);
        log.info("Notification deleted: user={}, id={}", userId, notificationId);
    }

    @Override
    public void deleteAll(NotificationType type, Long relatedId) {
        lambdaUpdate()
                .eq(Notification::getType, type)
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
