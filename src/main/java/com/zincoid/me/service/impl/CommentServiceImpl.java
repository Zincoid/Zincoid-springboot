package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.CommentMapper;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.converter.CommentConverter;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final UserService userService;
    private final NotificationService notificationService;
    private final MomentService momentService;
    private final ArticleService articleService;

    public CommentServiceImpl(@Lazy UserService userService, NotificationService notificationService,
                              @Lazy MomentService momentService, @Lazy ArticleService articleService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.momentService = momentService;
        this.articleService = articleService;
    }

    @Override
    public PageVO<CommentVO> list(RelatedType targetType, Long targetId, int page, int size) {
        IPage<Comment> rootPage = lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .isNull(Comment::getParentId)
                .orderByAsc(Comment::getCreatedAt)
                .page(Page.of(page, size));
        List<Comment> rootComments = rootPage.getRecords();
        Map<Long, Long> replyCounts = getReplyCounts(targetType, targetId, rootComments);
        List<CommentVO> roots = rootComments.stream()
                .map(c -> toCommentVO(c, List.of(), replyCounts.getOrDefault(c.getId(), 0L)))
                .toList();
        return PageVO.of(rootPage, roots);
    }

    @Override
    public List<CommentVO> replies(Long parentId) {
        Comment parent = getById(parentId);
        if (parent == null) return List.of();
        List<Long> descendantIds = collectDescendantIds(parentId);
        if (descendantIds.isEmpty()) return List.of();
        List<Comment> children = lambdaQuery()
                .in(Comment::getId, descendantIds)
                .orderByAsc(Comment::getCreatedAt)
                .list();
        return children.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .map(c -> buildReplyTree(c, children))
                .toList();
    }

    @Override
    public long count(RelatedType targetType, Long targetId) {
        return lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .count();
    }

    @Override
    @Transactional
    public CommentVO add(Long userId, RelatedType targetType, Long targetId, String content, Long parentId) {
        Comment comment = Comment.builder()
                .targetType(targetType)
                .targetId(targetId)
                .userId(userId)
                .content(content)
                .parentId(parentId)
                .build();
        save(comment);
        if (parentId != null) {
            Comment parent = getById(parentId);
            if (parent != null && !parent.getUserId().equals(userId))
                notificationService.notify(userId, parent.getUserId(),
                        NotificationType.REPLY, comment.getId());
        } else {
            Long authorId = null;
            if (targetType == RelatedType.MOMENT) {
                Moment m = momentService.getById(targetId);
                if (m != null) authorId = m.getUserId();
            } else {
                Article a = articleService.getById(targetId);
                if (a != null) authorId = a.getUserId();
            }
            if (authorId != null && !authorId.equals(userId))
                notificationService.notify(userId, authorId,
                        NotificationType.COMMENT, comment.getId());
        }
        notificationService.notify(userId, content, NotificationType.COMMENT_MENTION, comment.getId());
        log.info("Comment added: user={}, target={}:{}, id={}", userId, targetType, targetId, comment.getId());
        return toCommentVO(comment, List.of(), 0);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId, boolean isAdmin) {
        Comment comment = getById(commentId);
        if (comment == null)
            throw new BusinessException(404, "Comment not found");
        if (!isAdmin && !comment.getUserId().equals(userId))
            throw new BusinessException(403, "You can only delete your own comments");
        List<Long> allIds = collectDescendantIds(commentId);
        allIds.add(commentId);
        removeByIds(allIds);
        for (Long cid : allIds) {
            notificationService.deleteAll(NotificationType.COMMENT, cid);
            notificationService.deleteAll(NotificationType.REPLY, cid);
            notificationService.deleteAll(NotificationType.COMMENT_MENTION, cid);
        }
        log.info("Comment deleted: user={}, id={}", userId, commentId);
    }

    @Override
    @Transactional
    public void delete(RelatedType targetType, Long targetId) {
        List<Long> commentIds = lambdaQuery()
                .select(Comment::getId)
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .list()
                .stream().map(Comment::getId).toList();
        lambdaUpdate()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .remove();
        for (Long cid : commentIds) {
            notificationService.deleteAll(NotificationType.COMMENT, cid);
            notificationService.deleteAll(NotificationType.REPLY, cid);
            notificationService.deleteAll(NotificationType.COMMENT_MENTION, cid);
        }
        log.info("Comments deleted: target={}:{}", targetType, targetId);
    }

    // ──────── Tree builder ────────────────────────────────

    private Map<Long, Long> getReplyCounts(RelatedType targetType, Long targetId, List<Comment> roots) {
        if (roots.isEmpty()) return Map.of();
        List<Comment> allNonRoots = lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .isNotNull(Comment::getParentId)
                .select(Comment::getId, Comment::getParentId)
                .list();
        Map<Long, List<Long>> childrenMap = allNonRoots.stream()
                .collect(Collectors.groupingBy(Comment::getParentId,
                        Collectors.mapping(Comment::getId, Collectors.toList())));
        Map<Long, Long> result = new HashMap<>();
        for (Comment root : roots)
            result.put(root.getId(), countDescendants(root.getId(), childrenMap));
        return result;
    }

    private long countDescendants(Long parentId, Map<Long, List<Long>> childrenMap) {
        List<Long> children = childrenMap.getOrDefault(parentId, List.of());
        long total = children.size();
        for (Long childId : children)
            total += countDescendants(childId, childrenMap);
        return total;
    }

    private CommentVO buildReplyTree(Comment comment, List<Comment> all) {
        List<CommentVO> replies = all.stream()
                .filter(c -> comment.getId().equals(c.getParentId()))
                .map(c -> buildReplyTree(c, all))
                .toList();
        long totalDescendants = replies.stream()
                .mapToLong(r -> 1 + r.getReplyCount())
                .sum();
        return toCommentVO(comment, replies, totalDescendants);
    }

    private List<Long> collectDescendantIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Long> children = lambdaQuery()
                .select(Comment::getId)
                .eq(Comment::getParentId, parentId)
                .list()
                .stream().map(Comment::getId).toList();
        for (Long childId : children) {
            ids.add(childId);
            ids.addAll(collectDescendantIds(childId));
        }
        return ids;
    }

    private CommentVO toCommentVO(Comment comment, List<CommentVO> replies, long replyCount) {
        User user = userService.getById(comment.getUserId());
        return CommentConverter.INSTANCE.toVO(comment, user, replies, replyCount);
    }
}
