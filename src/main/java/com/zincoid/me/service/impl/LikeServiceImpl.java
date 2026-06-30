package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zincoid.me.mapper.LikeMapper;
import com.zincoid.me.model.po.Like;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    private final UserService userService;
    private final NotificationService notificationService;
    private final MomentService momentService;
    private final ArticleService articleService;

    public LikeServiceImpl(@Lazy UserService userService, @Lazy NotificationService notificationService,
                           @Lazy MomentService momentService, @Lazy ArticleService articleService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.momentService = momentService;
        this.articleService = articleService;
    }

    @Override
    public boolean liked(Long userId, RelatedType targetType, Long targetId) {
        if (userId == null || targetType == null || targetId == null) return false;
        return lambdaQuery()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)
                .exists();
    }

    @Override
    @Transactional
    public boolean toggle(Long userId, RelatedType targetType, Long targetId) {
        Like existing = lambdaQuery()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)
                .one();
        if (existing != null) {
            removeById(existing.getId());
            notificationService.deleteAll(NotificationType.LIKE, existing.getId());
            log.info("Like removed: user={}, target={}:{}", userId, targetType, targetId);
            return false;
        }
        Like like = Like.builder()
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .build();
        save(like);
        Long authorId = null;
        if (targetType == RelatedType.MOMENT) {
            Moment m = momentService.lambdaQuery().select(Moment::getUserId).eq(Moment::getId, targetId).one();
            if (m != null) authorId = m.getUserId();
        } else {
            Article a = articleService.lambdaQuery().select(Article::getUserId).eq(Article::getId, targetId).one();
            if (a != null) authorId = a.getUserId();
        }
        if (authorId != null && !authorId.equals(userId))
            notificationService.notify(userId, authorId, NotificationType.LIKE, like.getId());
        log.info("Like added: user={}, target={}:{}", userId, targetType, targetId);
        return true;
    }

    @Override
    public long count(RelatedType targetType, Long targetId) {
        return lambdaQuery()
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)
                .count();
    }

    @Override
    @Transactional
    public void delete(RelatedType targetType, Long targetId) {
        List<Long> likeIds = lambdaQuery()
                .select(Like::getId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)
                .list()
                .stream().map(Like::getId).toList();
        if (!likeIds.isEmpty()) {
            removeBatchByIds(likeIds);
            for (Long likeId : likeIds)
                notificationService.deleteAll(NotificationType.LIKE, likeId);
        }
        log.info("Likes deleted: target={}:{}", targetType, targetId);
    }

    @Override
    public List<LikerVO> getLikers(RelatedType targetType, Long targetId, int limit) {
        List<Like> likes = lambdaQuery()
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)
                .orderByDesc(Like::getCreatedAt)
                .page(Page.of(1, limit))
                .getRecords();
        return likes.stream().map(like -> {
            User user = userService.getById(like.getUserId());
            if (user == null) return null;
            return LikerVO.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build();
        }).filter(Objects::nonNull).toList();
    }
}
