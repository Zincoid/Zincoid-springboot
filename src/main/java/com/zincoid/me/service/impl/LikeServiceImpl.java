package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;
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
        if (userId == null) return false;
        long c = count(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));
        return c > 0;
    }

    @Override
    @Transactional
    public boolean toggle(Long userId, RelatedType targetType, Long targetId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId);

        Like existing = getOne(wrapper);
        if (existing != null) {
            removeById(existing.getId());
            return false;
        }

        Like like = Like.builder()
                .userId(userId)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(LocalDateTime.now())
                .build();
        save(like);
        return true;
    }

    @Override
    public long count(RelatedType targetType, Long targetId) {
        return count(new LambdaQueryWrapper<Like>()
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));
    }

    @Override
    @Transactional
    public void delete(RelatedType targetType, Long targetId) {
        remove(new LambdaQueryWrapper<Like>()
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));
    }

    @Override
    public List<LikerVO> getLikers(RelatedType targetType, Long targetId, int limit) {
        List<Like> likes = page(new Page<>(1, limit),
                new LambdaQueryWrapper<Like>()
                        .eq(Like::getTargetType, targetType)
                        .eq(Like::getTargetId, targetId)
                        .orderByDesc(Like::getCreatedAt)).getRecords();

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
