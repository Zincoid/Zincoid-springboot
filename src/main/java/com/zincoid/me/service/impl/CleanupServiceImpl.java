package com.zincoid.me.service.impl;

import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.*;
import com.zincoid.me.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupServiceImpl {

    private final UserService userService;
    private final MessageService messageService;
    private final MomentService momentService;
    private final ArticleService articleService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final NotificationService notificationService;
    private final FileService fileService;

    @Transactional
    public Map<String, Integer> cleanup() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("message", cleanMessages());
        result.put("moment", cleanMoments());
        result.put("article", cleanArticles());
        result.put("like", cleanLikes());
        result.put("comment", cleanComments());
        result.put("notification", cleanNotifications());
        log.info("Cleanup done: {}", result);
        return result;
    }

    public void cleanup(boolean isLogic) {
        fileService.cleanup(isLogic);
    }

    // ── helpers ──

    private Set<Long> userIds() {
        return new HashSet<>(userService.lambdaQuery().select(User::getId).list()
                .stream().map(User::getId).toList());
    }

    private Set<Long> momentIds() {
        return new HashSet<>(momentService.lambdaQuery().select(Moment::getId).list()
                .stream().map(Moment::getId).toList());
    }

    private Set<Long> articleIds() {
        return new HashSet<>(articleService.lambdaQuery().select(Article::getId).list()
                .stream().map(Article::getId).toList());
    }

    private Set<Long> commentIds() {
        return new HashSet<>(commentService.lambdaQuery().select(Comment::getId).list()
                .stream().map(Comment::getId).toList());
    }

    // ── cleaners ──

    private int cleanMessages() {
        Set<Long> users = userIds();
        List<Long> ids = messageService.lambdaQuery()
                .select(Message::getId, Message::getUserId).list().stream()
                .filter(m -> !users.contains(m.getUserId()))
                .map(Message::getId).toList();
        if (!ids.isEmpty()) messageService.removeBatchByIds(ids);
        return ids.size();
    }

    private int cleanMoments() {
        Set<Long> users = userIds();
        List<Long> ids = momentService.lambdaQuery()
                .select(Moment::getId, Moment::getUserId).list().stream()
                .filter(m -> !users.contains(m.getUserId()))
                .map(Moment::getId).toList();
        if (!ids.isEmpty()) momentService.removeBatchByIds(ids);
        return ids.size();
    }

    private int cleanArticles() {
        Set<Long> users = userIds();
        List<Long> ids = articleService.lambdaQuery()
                .select(Article::getId, Article::getUserId).list().stream()
                .filter(a -> !users.contains(a.getUserId()))
                .map(Article::getId).toList();
        if (!ids.isEmpty()) articleService.removeBatchByIds(ids);
        return ids.size();
    }

    private int cleanLikes() {
        Set<Long> users = userIds();
        Set<Long> moments = momentIds();
        Set<Long> articles = articleIds();
        List<Long> ids = likeService.lambdaQuery()
                .select(Like::getId, Like::getTargetType, Like::getTargetId, Like::getUserId).list().stream()
                .filter(l -> !users.contains(l.getUserId())
                        || l.getTargetType() == RelatedType.MOMENT && !moments.contains(l.getTargetId())
                        || l.getTargetType() == RelatedType.ARTICLE && !articles.contains(l.getTargetId()))
                .map(Like::getId).toList();
        if (!ids.isEmpty()) likeService.removeBatchByIds(ids);
        return ids.size();
    }

    private int cleanComments() {
        Set<Long> users = userIds();
        Set<Long> moments = momentIds();
        Set<Long> articles = articleIds();
        Set<Long> roots = commentIds();
        List<Long> ids = commentService.lambdaQuery()
                .select(Comment::getId, Comment::getTargetType, Comment::getTargetId,
                        Comment::getUserId, Comment::getRootId).list().stream()
                .filter(c -> !users.contains(c.getUserId())
                        || c.getTargetType() == RelatedType.MOMENT && !moments.contains(c.getTargetId())
                        || c.getTargetType() == RelatedType.ARTICLE && !articles.contains(c.getTargetId())
                        || !roots.contains(c.getRootId()))
                .map(Comment::getId).toList();
        if (!ids.isEmpty()) commentService.removeBatchByIds(ids);
        return ids.size();
    }

    private int cleanNotifications() {
        Set<Long> users = userIds();
        Set<Long> moments = momentIds();
        List<Long> ids = notificationService.lambdaQuery()
                .select(Notification::getId, Notification::getRelatedType, Notification::getRelatedId,
                        Notification::getSenderId, Notification::getReceiverId).list().stream()
                .filter(n -> {
                    if (!users.contains(n.getSenderId()) || !users.contains(n.getReceiverId()))
                        return true;
                    if (n.getRelatedType() == null) return true;
                    if (n.getRelatedType() == NotificationType.SYSTEM)
                        return n.getRelatedId() == null || n.getRelatedId() != -1;
                    return switch (n.getRelatedType()) {
                        case COMMENT, REPLY, COMMENT_MENTION ->
                                commentService.getById(n.getRelatedId()) == null;
                        case MOMENT_MENTION -> !moments.contains(n.getRelatedId());
                        case CHAT_MENTION -> messageService.getById(n.getRelatedId()) == null;
                        case LIKE -> likeService.getById(n.getRelatedId()) == null;
                        default -> false;
                    };
                })
                .map(Notification::getId).toList();
        if (!ids.isEmpty()) notificationService.removeBatchByIds(ids);
        return ids.size();
    }
}
