package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.CommentMapper;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.converter.CommentConverter;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<CommentVO> list(RelatedType targetType, Long targetId) {
        List<Comment> comments = lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .orderByAsc(Comment::getCreatedAt)
                .list();
        return buildCommentTree(comments);
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
                notificationService.add(
                        userId, parent.getUserId(),
                        targetType, targetId, comment.getId());
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
                notificationService.add(
                        userId, authorId,
                        targetType, targetId, comment.getId());
        }
        return toCommentVO(comment, List.of());
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId, boolean isAdmin) {
        Comment comment = getById(commentId);
        if (comment == null)
            throw new BusinessException(404, "Comment not found");
        if (!isAdmin && !comment.getUserId().equals(userId))
            throw new BusinessException(403, "You can only delete your own comments");
        lambdaUpdate().eq(Comment::getParentId, commentId).remove();
        removeById(commentId);
        log.info("Comment deleted: {}", commentId);
    }

    @Override
    @Transactional
    public void delete(RelatedType targetType, Long targetId) {
        lambdaUpdate()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .remove();
        log.info("Deleted all comments for {}:{}", targetType, targetId);
    }

    // ──────── Tree builder ────────────────────────────────

    private List<CommentVO> buildCommentTree(List<Comment> comments) {
        List<Comment> roots = comments.stream()
                .filter(c -> c.getParentId() == null)
                .toList();
        return roots.stream()
                .map(root -> buildNode(root, comments))
                .toList();
    }

    private CommentVO buildNode(Comment comment, List<Comment> all) {
        List<CommentVO> replies = all.stream()
                .filter(c -> comment.getId().equals(c.getParentId()))
                .map(c -> buildNode(c, all))
                .toList();
        return toCommentVO(comment, replies);
    }

    // ──────── Private tool ────────────────────────────────

    private CommentVO toCommentVO(Comment comment, List<CommentVO> replies) {
        User user = userService.getById(comment.getUserId());
        return CommentConverter.INSTANCE.toVO(comment, user, replies);
    }
}
