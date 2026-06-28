package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zincoid.me.mapper.LikeMapper;
import com.zincoid.me.model.po.Like;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    private final UserService userService;

    public LikeServiceImpl(@Lazy UserService userService) {
        this.userService = userService;
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
            return false;
        }
        Like like = Like.builder()
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .build();
        save(like);
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
        lambdaUpdate()
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId)
                .remove();
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
