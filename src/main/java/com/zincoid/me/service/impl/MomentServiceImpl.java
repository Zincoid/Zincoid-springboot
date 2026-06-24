package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.model.vo.PageVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.me.converter.MomentConverter;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.MomentMapper;
import com.zincoid.me.model.dto.MomentCreateRequest;
import com.zincoid.me.model.dto.MomentUpdateRequest;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.model.vo.MomentDetailVO;
import com.zincoid.me.model.vo.MomentCardVO;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import com.zincoid.me.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class MomentServiceImpl extends ServiceImpl<MomentMapper, Moment> implements MomentService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserService userService;
    private final CommentService commentService;
    private final FileService fileService;
    private final LikeService likeService;

    public MomentServiceImpl(@Lazy UserService userService, CommentService commentService,
                             FileService fileService, LikeService likeService) {
        this.userService = userService;
        this.commentService = commentService;
        this.fileService = fileService;
        this.likeService = likeService;
    }

    @Override
    @Transactional
    public MomentCardVO create(Long userId, MomentCreateRequest request) {
        Moment moment = Moment.builder()
                .userId(userId)
                .content(request.getContent())
                .images(JsonUtil.toJson(request.getImages()))
                .build();
        save(moment);
        log.info("Moment created by user {}: {}", userId, moment.getId());
        if (request.getImages() != null && !request.getImages().isEmpty())
            fileService.link(request.getImages(), RelatedType.MOMENT, moment.getId());
        return buildCardVO(moment);
    }

    @Override
    @Transactional
    public MomentCardVO update(Long userId, Long momentId, MomentUpdateRequest request) {
        Moment moment = getById(momentId);
        if (moment == null || moment.getStatus() == Status.DISABLED) {
            throw new BusinessException(404, "Moment not found");
        }
        if (!moment.getUserId().equals(userId)) {
            throw new BusinessException(403, "You can only edit your own moments");
        }

        if (request.getContent() != null) moment.setContent(request.getContent());
        if (request.getImages() != null) {
            List<String> oldImages = parseImages(moment.getImages());
            Set<String> oldSet = new HashSet<>(oldImages);
            Set<String> newSet = new HashSet<>(request.getImages());
            for (String oldPath : oldSet) {
                if (!newSet.contains(oldPath)) {
                    fileService.delete(oldPath);
                }
            }
            moment.setImages(JsonUtil.toJson(request.getImages()));
            List<String> newPaths = new ArrayList<>(newSet);
            if (!newPaths.isEmpty()) {
                fileService.link(newPaths, RelatedType.MOMENT, moment.getId());
            }
        }

        updateById(moment);
        return buildCardVO(moment);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long momentId, boolean isAdmin) {
        Moment moment = getById(momentId);
        if (moment == null || moment.getStatus() == Status.DISABLED) {
            throw new BusinessException(404, "Moment not found");
        }
        if (!isAdmin && !moment.getUserId().equals(userId)) {
            throw new BusinessException(403, "No permission to delete this moment");
        }

        likeService.delete(RelatedType.MOMENT, momentId);
        commentService.delete(RelatedType.MOMENT, momentId);
        fileService.delete(RelatedType.MOMENT, momentId);
        removeById(momentId);
        log.info("Moment deleted: {}", momentId);
    }

    @Override
    public void pin(Long momentId) {
        Moment moment = getById(momentId);
        if (moment == null || moment.getStatus() == Status.DISABLED) {
            throw new BusinessException(404, "Moment not found");
        }
        moment.setIsPinned(true);
        updateById(moment);
    }

    @Override
    public void unpin(Long momentId) {
        Moment moment = getById(momentId);
        if (moment == null) {
            throw new BusinessException(404, "Moment not found");
        }
        moment.setIsPinned(false);
        updateById(moment);
    }

    @Override
    public PageVO<MomentCardVO> list(int page, int size) {
        Page<Moment> momentPage = page(
                new Page<>(page, size),
                new LambdaQueryWrapper<Moment>()
                        .eq(Moment::getStatus, Status.ACTIVE)
                        .orderByDesc(Moment::getIsPinned)
                        .orderByDesc(Moment::getCreatedAt));
        return PageVO.of(momentPage, this::buildCardVO);
    }

    @Override
    public PageVO<MomentCardVO> list(Long userId, int page, int size) {
        Page<Moment> momentPage = page(
                new Page<>(page, size),
                new LambdaQueryWrapper<Moment>()
                        .eq(Moment::getUserId, userId)
                        .eq(Moment::getStatus, Status.ACTIVE)
                        .orderByDesc(Moment::getCreatedAt));
        return PageVO.of(momentPage, this::buildCardVO);
    }

    @Override
    public MomentDetailVO get(Long momentId) {
        Moment moment = getById(momentId);
        if (moment == null || moment.getStatus() == Status.DISABLED)
            throw new BusinessException(404, "Moment not found");
        List<CommentVO> comments = commentService.list(RelatedType.MOMENT, momentId);
        User user = userService.getById(moment.getUserId());
        long likeCount = likeService.count(RelatedType.MOMENT, momentId);
        boolean isLiked = likeService.liked(AuthCtx.getUserId(), RelatedType.MOMENT, momentId);
        List<LikerVO> recentLikers = likeService.getLikers(RelatedType.MOMENT, momentId, 5);
        baseMapper.addViewCount(momentId);
        moment.setViewCount(moment.getViewCount() + 1);
        return MomentConverter.INSTANCE.toDetailVO(moment, user, isLiked, likeCount, comments, recentLikers);
    }

    private MomentCardVO buildCardVO(Moment moment) {
        User user = userService.getById(moment.getUserId());
        int commentCount = (int) commentService.count(RelatedType.MOMENT, moment.getId());
        long likeCount = likeService.count(RelatedType.MOMENT, moment.getId());
        boolean isLiked = likeService.liked(AuthCtx.getUserId(), RelatedType.MOMENT, moment.getId());
        return MomentConverter.INSTANCE.toCardVO(moment, user, isLiked, likeCount, commentCount);
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
