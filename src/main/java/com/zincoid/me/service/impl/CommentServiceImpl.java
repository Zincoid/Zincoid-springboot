package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.CommentMapper;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.converter.CommentConverter;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserService userService;

    public CommentServiceImpl(CommentMapper commentMapper, @Lazy UserService userService) {
        this.commentMapper = commentMapper;
        this.userService = userService;
    }

    @Override
    public List<CommentVO> list(RelatedType targetType, Long targetId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId)
                        .orderByAsc(Comment::getCreatedAt));

        return buildCommentTree(comments);
    }

    @Override
    public long count(RelatedType targetType, Long targetId) {
        return commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId));
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

        commentMapper.insert(comment);
        return toCommentVO(comment, List.of());
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId, boolean isAdmin) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "Comment not found");
        }
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "You can only delete your own comments");
        }
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, commentId));
        commentMapper.deleteById(commentId);
        log.info("Comment deleted: {}", commentId);
    }

    @Override
    @Transactional
    public void delete(RelatedType targetType, Long targetId) {
        commentMapper.delete(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId));
        log.info("Deleted all comments for {}:{}", targetType, targetId);
    }

    // ---- Tree builder ----

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

    private CommentVO toCommentVO(Comment comment, List<CommentVO> replies) {
        User user = userService.getById(comment.getUserId());
        return CommentConverter.INSTANCE.toVO(comment, user, replies);
    }
}
