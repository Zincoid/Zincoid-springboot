package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.model.enums.type.BizType;
import com.zincoid.me.module.core.DataInitializer;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.dict.Error;
import com.zincoid.me.mapper.CommentMapper;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.type.NotificationType;
import com.zincoid.me.model.enums.state.Status;
import com.zincoid.me.converter.CommentConverter;
import com.zincoid.me.model.vo.CommentCardVO;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.AiService;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import com.zincoid.me.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final UserService userService;
    private final MomentService momentService;
    private final ArticleService articleService;
    private final RepoService repoService;
    private final LikeService likeService;
    private final NotificationService notificationService;
    private final AiService aiService;

    public CommentServiceImpl(UserService userService,
                              @Lazy MomentService momentService,
                              @Lazy ArticleService articleService,
                              @Lazy RepoService repoService,
                              LikeService likeService,
                              NotificationService notificationService,
                              AiService aiService) {
        this.userService = userService;
        this.momentService = momentService;
        this.articleService = articleService;
        this.repoService = repoService;
        this.likeService = likeService;
        this.notificationService = notificationService;
        this.aiService = aiService;
    }

    @Override
    public PageVO<CommentVO> list(BizType targetType, Long targetId, boolean hot, int page, int size) {
        validate(targetType, targetId);
        IPage<Comment> rootPage = hot
                ? baseMapper.selectRootsByLikes(Page.of(page, size), targetType, targetId, BizType.COMMENT)
                : lambdaQuery()
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getTargetId, targetId)
                        .isNull(Comment::getParentId)
                        .orderByDesc(Comment::getIsPinned)
                        .orderByDesc(Comment::getCreatedAt)
                        .page(Page.of(page, size));
        List<Comment> rootComments = rootPage.getRecords();
        Map<Long, Long> replyCounts = getReplyCounts(targetType, targetId, rootComments);
        List<CommentVO> roots = rootComments.stream()
                .map(c -> toCommentVO(c, List.of(), replyCounts.getOrDefault(c.getId(), 0L)))
                .toList();
        return PageVO.of(rootPage, roots);
    }

    @Override
    public List<CommentCardVO> home(int size) {
        List<Comment> comments = lambdaQuery()
                .orderByDesc(Comment::getCreatedAt)
                .last("LIMIT " + size)
                .list();
        List<CommentCardVO> result = new ArrayList<>();
        for (Comment comment : comments) {
            if (!isActive(comment.getTargetType(), comment.getTargetId())) continue;
            result.add(toCommentCardVO(comment));
        }
        return result;
    }

    @Override
    public List<CommentVO> replies(Long parentId) {
        Comment parent = getById(parentId);
        if (parent == null) return List.of();
        validate(parent.getTargetType(), parent.getTargetId());
        List<Comment> descendants = lambdaQuery()
                .eq(Comment::getRootId, parent.getRootId())
                .ne(Comment::getId, parentId)
                .orderByAsc(Comment::getCreatedAt)
                .list();
        return descendants.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .map(c -> buildReplyTree(c, descendants))
                .toList();
    }

    @Override
    public CommentCardVO card(Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null || !isActive(comment.getTargetType(), comment.getTargetId()))
            throw new BusinessException(Error.COMMENT_NOT_FOUND);
        return toCommentCardVO(comment);
    }

    @Override
    public long count(BizType targetType, Long targetId) {
        return lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .count();
    }

    @Override
    @Transactional
    public CommentVO add(Long userId, BizType targetType, Long targetId, String content, Long parentId) {
        validate(targetType, targetId);
        Comment parent = parentId != null ? getById(parentId) : null;
        Comment comment = Comment.builder()
                .targetType(targetType)
                .targetId(targetId)
                .userId(userId)
                .content(content)
                .parentId(parentId)
                .rootId(parent != null ? parent.getRootId() : 0L)
                .build();
        save(comment);
        if (parent == null) {
            comment.setRootId(comment.getId());
            updateById(comment);
        }
        if (parent != null && !parent.getUserId().equals(userId))
            notificationService.notify(userId, parent.getUserId(),
                    NotificationType.REPLY, comment.getId());
        if (parentId == null) {
            Long authorId = getAuthor(targetType, targetId);
            if (authorId != null && !authorId.equals(userId))
                notificationService.notify(userId, authorId,
                        NotificationType.COMMENT, comment.getId());
        }
        boolean isAiMentioned = false;
        for (String name : StrUtil.extractAts(content)) {
            if (DataInitializer.AI_USERNAME.equals(name)) { isAiMentioned = true; continue; }
            notificationService.notifyAt(userId, name, NotificationType.COMMENT_MENTION, comment.getId());
        }
        if (isAiMentioned)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiService.comment(userId, comment.getId());
                }
            });
        log.info("Comment added: user={}, target={}:{}, id={}", userId, targetType, targetId, comment.getId());
        return toCommentVO(comment, List.of(), 0);
    }

    @Override
    @Transactional
    public void pin(Long userId, Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null)
            throw new BusinessException(Error.COMMENT_NOT_FOUND);
        if (comment.getParentId() != null)
            throw new BusinessException(Error.PIN_ROOT_COMMENT_ONLY);
        if (!Objects.equals(userId, getAuthor(comment.getTargetType(), comment.getTargetId())))
            throw new BusinessException(Error.NO_PERMISSION_PIN_COMMENT);
        lambdaUpdate()
                .eq(Comment::getTargetType, comment.getTargetType())
                .eq(Comment::getTargetId, comment.getTargetId())
                .eq(Comment::getIsPinned, true)
                .ne(Comment::getId, commentId)
                .set(Comment::getIsPinned, false)
                .update();
        lambdaUpdate()
                .eq(Comment::getId, commentId)
                .set(Comment::getIsPinned, true)
                .update();
        log.info("Comment pinned: user={}, id={}", userId, commentId);
    }

    @Override
    @Transactional
    public void unpin(Long userId, Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null)
            throw new BusinessException(Error.COMMENT_NOT_FOUND);
        if (!Objects.equals(userId, getAuthor(comment.getTargetType(), comment.getTargetId())))
            throw new BusinessException(Error.NO_PERMISSION_UNPIN_COMMENT);
        lambdaUpdate()
                .eq(Comment::getId, commentId)
                .set(Comment::getIsPinned, false)
                .update();
        log.info("Comment unpinned: user={}, id={}", userId, commentId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId, boolean isAdmin) {
        Comment comment = getById(commentId);
        if (comment == null)
            throw new BusinessException(Error.COMMENT_NOT_FOUND);
        if (!isAdmin && !comment.getUserId().equals(userId)
                && !Objects.equals(userId, getAuthor(comment.getTargetType(), comment.getTargetId())))
            throw new BusinessException(Error.NO_PERMISSION_DELETE_COMMENT);
        List<Long> allIds;
        if (comment.getId().equals(comment.getRootId())) {
            allIds = lambdaQuery()
                    .select(Comment::getId)
                    .eq(Comment::getRootId, commentId)
                    .list()
                    .stream().map(Comment::getId)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            allIds = new ArrayList<>();
            List<Comment> treeNodes = lambdaQuery()
                    .select(Comment::getId, Comment::getParentId)
                    .eq(Comment::getRootId, comment.getRootId())
                    .list();
            Map<Long, List<Long>> childrenMap = treeNodes.stream()
                    .filter(c -> c.getParentId() != null)
                    .collect(Collectors.groupingBy(Comment::getParentId,
                            Collectors.mapping(Comment::getId, Collectors.toList())));
            collectDescendants(commentId, childrenMap, allIds);
        }
        allIds.add(commentId);
        removeBatchByIds(allIds);
        for (Long cid : allIds) {
            likeService.delete(BizType.COMMENT, cid);
            notificationService.deleteAll(NotificationType.COMMENT, cid);
            notificationService.deleteAll(NotificationType.REPLY, cid);
            notificationService.deleteAll(NotificationType.COMMENT_MENTION, cid);
        }
        log.info("Comment deleted: user={}, id={}", userId, commentId);
    }

    @Override
    @Transactional
    public void delete(BizType targetType, Long targetId) {
        List<Long> commentIds = lambdaQuery()
                .select(Comment::getId)
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .list()
                .stream().map(Comment::getId).toList();
        if (!commentIds.isEmpty()) {
            removeBatchByIds(commentIds);
            for (Long cid : commentIds) {
                likeService.delete(BizType.COMMENT, cid);
                notificationService.deleteAll(NotificationType.COMMENT, cid);
                notificationService.deleteAll(NotificationType.REPLY, cid);
                notificationService.deleteAll(NotificationType.COMMENT_MENTION, cid);
            }
        }
        log.info("Comments deleted: target={}:{}", targetType, targetId);
    }

    // ──────── Target validation ───────────────────────────

    private boolean isActive(BizType targetType, Long targetId) {
        if (targetType == BizType.MOMENT) {
            Moment m = momentService.lambdaQuery().select(Moment::getStatus).eq(Moment::getId, targetId).one();
            return m != null && m.getStatus() == Status.ACTIVE;
        } else if (targetType == BizType.ARTICLE) {
            Article a = articleService.lambdaQuery().select(Article::getStatus).eq(Article::getId, targetId).one();
            return a != null && a.getStatus() == Status.ACTIVE;
        } else if (targetType == BizType.REPO) {
            Repo r = repoService.lambdaQuery().select(Repo::getStatus).eq(Repo::getId, targetId).one();
            return r != null && r.getStatus() == Status.ACTIVE;
        }
        return false;
    }

    private void validate(BizType targetType, Long targetId) {
        if (targetType == BizType.MOMENT) {
            Moment m = momentService.lambdaQuery().select(Moment::getStatus).eq(Moment::getId, targetId).one();
            if (m == null || m.getStatus() == Status.DISABLED)
                throw new BusinessException(Error.MOMENT_NOT_FOUND);
        } else if (targetType == BizType.ARTICLE) {
            Article a = articleService.lambdaQuery().select(Article::getStatus).eq(Article::getId, targetId).one();
            if (a == null || a.getStatus() == Status.DISABLED)
                throw new BusinessException(Error.ARTICLE_NOT_FOUND);
        } else if (targetType == BizType.REPO) {
            Repo r = repoService.lambdaQuery().select(Repo::getStatus).eq(Repo::getId, targetId).one();
            if (r == null || r.getStatus() == Status.DISABLED)
                throw new BusinessException(Error.REPO_NOT_FOUND);
        } else throw new IllegalArgumentException("Invalid target type");
    }

    private Long getAuthor(BizType targetType, Long targetId) {
        if (targetType == BizType.MOMENT) {
            Moment m = momentService.getById(targetId);
            return m == null ? null : m.getUserId();
        } else if (targetType == BizType.ARTICLE) {
            Article a = articleService.getById(targetId);
            return a == null ? null : a.getUserId();
        } else if (targetType == BizType.REPO) {
            Repo r = repoService.getById(targetId);
            return r == null ? null : r.getUserId();
        }
        return null;
    }

    // ──────── Tree builder ────────────────────────────────

    private Map<Long, Long> getReplyCounts(BizType targetType, Long targetId, List<Comment> roots) {
        if (roots.isEmpty()) return Map.of();
        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        return lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .in(Comment::getRootId, rootIds)
                .isNotNull(Comment::getParentId)
                .list()
                .stream()
                .collect(Collectors.groupingBy(Comment::getRootId, Collectors.counting()));
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

    private void collectDescendants(Long parentId, Map<Long, List<Long>> childrenMap, List<Long> result) {
        List<Long> children = childrenMap.getOrDefault(parentId, List.of());
        for (Long childId : children) {
            result.add(childId);
            collectDescendants(childId, childrenMap, result);
        }
    }

    private CommentVO toCommentVO(Comment comment, List<CommentVO> replies, long replyCount) {
        User user = userService.getById(comment.getUserId());
        Long viewerId = AuthCtx.getUserId();
        return CommentConverter.INSTANCE.toVO(comment, user, replies, replyCount,
                likeService.count(BizType.COMMENT, comment.getId()),
                likeService.liked(viewerId, BizType.COMMENT, comment.getId()));
    }

    private CommentCardVO toCommentCardVO(Comment comment) {
        User user = userService.getById(comment.getUserId());
        User parentUser = null;
        if (comment.getParentId() != null) {
            Comment parent = getById(comment.getParentId());
            if (parent != null) parentUser = userService.getById(parent.getUserId());
        }
        return CommentConverter.INSTANCE.toCardVO(comment, user, parentUser);
    }
}
