package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.RepoMapper;
import com.zincoid.me.converter.RepoConverter;
import com.zincoid.me.model.dto.RepoCreateRequest;
import com.zincoid.me.model.dto.RepoItemAddRequest;
import com.zincoid.me.model.dto.RepoUpdateRequest;
import com.zincoid.me.model.enums.*;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.po.RepoItem;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoCardVO;
import com.zincoid.me.model.vo.RepoDetailVO;
import com.zincoid.me.model.vo.RepoItemVO;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.GitHubService;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.RepoItemService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import com.zincoid.me.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepoServiceImpl extends ServiceImpl<RepoMapper, Repo> implements RepoService {

    private final FileService fileService;
    private final UserService userService;
    private final RepoItemService repoItemService;
    private final RepoAccessService repoAccessService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final NotificationService notificationService;
    private final GitHubService gitHubService;

    @Override
    @Transactional
    public RepoDetailVO create(Long userId, RepoCreateRequest request) {
        Repo repo = Repo.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .url(request.getUrl())
                .tags(JsonUtil.toJson(request.getTags()))
                .coverImage(request.getCoverImage() != null && !request.getCoverImage().isBlank()
                        ? request.getCoverImage() : null)
                .status(Status.ACTIVE)
                .build();
        save(repo);
        if (repo.getCoverImage() != null)
            fileService.link(List.of(repo.getCoverImage()), RelatedType.REPO, repo.getId());
        log.info("Repo created: user={}, id={}, type={}", userId, repo.getId(), repo.getType());
        return buildDetailVO(repo);
    }

    @Override
    @Transactional
    public RepoDetailVO update(Long userId, Long repoId, RepoUpdateRequest request) {
        Repo repo = getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (!repo.getUserId().equals(userId))
            throw new BusinessException(403, "You can only edit your own repos");
        if (request.getName() != null) repo.setName(request.getName());
        if (request.getDescription() != null) repo.setDescription(request.getDescription());
        if (request.getUrl() != null) repo.setUrl(request.getUrl());
        if (request.getTags() != null) repo.setTags(JsonUtil.toJson(request.getTags()));
        if (request.getCoverImage() != null) {
            String newCover = request.getCoverImage().isBlank() ? null : request.getCoverImage();
            String oldCover = repo.getCoverImage();
            repo.setCoverImage(newCover);
            if (newCover == null)
                lambdaUpdate().set(Repo::getCoverImage, null).eq(Repo::getId, repoId).update();
            if (oldCover != null && !oldCover.equals(newCover))
                fileService.delete(oldCover);
        }
        if (request.getVisibility() != null) {
            if (repo.getVisibility() == Visibility.RESTRICTED && request.getVisibility() != Visibility.RESTRICTED)
                repoAccessService.lambdaUpdate().eq(RepoAccess::getRepoId, repoId).remove();
            repo.setVisibility(request.getVisibility());
        }
        updateById(repo);
        if (repo.getCoverImage() != null && !repo.getCoverImage().isBlank())
            fileService.link(List.of(repo.getCoverImage()), RelatedType.REPO, repo.getId());
        log.info("Repo updated: user={}, id={}", userId, repoId);
        return buildDetailVO(repo);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long repoId, boolean isAdmin) {
        Repo repo = getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (!isAdmin && !repo.getUserId().equals(userId))
            throw new BusinessException(403, "No permission to delete this repo");
        repoItemService.deleteByRepoId(repoId);
        likeService.delete(RelatedType.REPO, repoId);
        commentService.delete(RelatedType.REPO, repoId);
        notificationService.deleteAll(NotificationType.ACCESS_REQUEST, repoId);
        notificationService.deleteAll(NotificationType.ACCESS_APPROVED, repoId);
        notificationService.deleteAll(NotificationType.ACCESS_REJECTED, repoId);
        removeById(repoId);
        log.info("Repo deleted: user={}, admin={}, id={}", userId, isAdmin, repoId);
    }

    @Override
    @Transactional
    public RepoItemVO addItem(Long userId, Long repoId, RepoItemAddRequest request) {
        Repo repo = getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (!repo.getUserId().equals(userId))
            throw new BusinessException(403, "You can only edit your own repos");
        return repoItemService.add(repoId, request.getFileId(), request.getName());
    }

    @Override
    @Transactional
    public void deleteItem(Long userId, Long repoId, Long itemId) {
        Repo repo = getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (!repo.getUserId().equals(userId))
            throw new BusinessException(403, "You can only edit your own repos");
        RepoItem item = repoItemService.getById(itemId);
        if (item == null || !item.getRepoId().equals(repoId))
            throw new BusinessException(404, "Item not found");
        repoItemService.delete(itemId);
        log.info("Repo item deleted: repo={}, item={}", repoId, itemId);
    }

    @Override
    @Transactional
    public void swapItems(Long userId, Long repoId, Long itemIdA, Long itemIdB) {
        Repo repo = getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (!repo.getUserId().equals(userId))
            throw new BusinessException(403, "You can only edit your own repos");
        repoItemService.swap(repoId, itemIdA, itemIdB);
    }

    @Override
    public PageVO<RepoCardVO> list(RepoType type, String keyword, int page, int size) {
        Page<Repo> repoPage = lambdaQuery()
                .eq(Repo::getStatus, Status.ACTIVE)
                .ne(Repo::getVisibility, Visibility.PRIVATE)
                .eq(type != null, Repo::getType, type)
                .like(keyword != null && !keyword.isBlank(), Repo::getName, keyword)
                .orderByDesc(Repo::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(repoPage, this::buildCardVO);
    }

    @Override
    public PageVO<RepoCardVO> list(Long userId, RepoType type, int page, int size) {
        Long viewerId = AuthCtx.getUserId();
        boolean isOwner = viewerId != null && viewerId.equals(userId);
        boolean isAdmin = viewerId != null && AuthCtx.getRole() == Role.ADMIN;
        var wrapper = lambdaQuery()
                .eq(Repo::getStatus, Status.ACTIVE)
                .eq(Repo::getUserId, userId)
                .eq(type != null, Repo::getType, type)
                .ne(!isOwner && !isAdmin, Repo::getVisibility, Visibility.PRIVATE)
                .orderByDesc(Repo::getCreatedAt);
        Page<Repo> repoPage = wrapper.page(Page.of(page, size));
        return PageVO.of(repoPage, this::buildCardVO);
    }

    @Override
    public RepoDetailVO get(Long repoId) {
        Repo repo = getById(repoId);
        if (repo == null || repo.getStatus() == Status.DISABLED)
            throw new BusinessException(404, "Repo not found");
        Long viewerId = AuthCtx.getUserId();
        boolean isAdmin = viewerId != null && AuthCtx.getRole() == Role.ADMIN;
        if (repo.getVisibility() == Visibility.PRIVATE
                && !isAdmin
                && (viewerId == null || !viewerId.equals(repo.getUserId())))
            throw new BusinessException(404, "Repo is private");
        boolean isDenied = repo.getVisibility() == Visibility.RESTRICTED
                && !isAdmin
                && (viewerId == null || !viewerId.equals(repo.getUserId()))
                && !repoAccessService.authorize(viewerId, repoId);
        if (!isDenied) {
            baseMapper.addViewCount(repoId);
            repo.setViewCount(repo.getViewCount() != null ? repo.getViewCount() + 1 : 1L);
        }
        RepoDetailVO vo = buildDetailVO(repo);
        if (isDenied) {
            vo.setRestricted(true);
            vo.setUrl(null);
            vo.setGithub(null);
        }
        return vo;
    }

    @Override
    public PageVO<RepoItemVO> items(Long repoId, int page, int size) {
        Repo repo = getById(repoId);
        if (repo == null || repo.getStatus() == Status.DISABLED)
            throw new BusinessException(404, "Repo not found");
        Long viewerId = AuthCtx.getUserId();
        boolean isAdmin = viewerId != null && AuthCtx.getRole() == Role.ADMIN;
        if (repo.getVisibility() == Visibility.PRIVATE
                && !isAdmin
                && (viewerId == null || !viewerId.equals(repo.getUserId())))
            throw new BusinessException(404, "Repo is private");
        boolean isDenied = repo.getVisibility() == Visibility.RESTRICTED
                && !isAdmin
                && (viewerId == null || !viewerId.equals(repo.getUserId()))
                && !repoAccessService.authorize(viewerId, repoId);
        if (isDenied || repo.getType() == RepoType.CODE)
            return PageVO.<RepoItemVO>builder()
                    .records(List.of()).total(0).page(page).size(size).pages(0)
                    .build();
        return repoItemService.list(repoId, page, size);
    }

    // ──────── Private tool ────────────────────────────────

    private boolean isDefaultCover(Repo repo) {
        return repo.getCoverImage() == null || repo.getCoverImage().isBlank();
    }

    private String coverOrDefault(Repo repo) {
        if (!isDefaultCover(repo)) return repo.getCoverImage();
        return repoItemService.firstImageUrl(repo.getId());
    }

    private RepoCardVO buildCardVO(Repo repo) {
        User user = userService.getById(repo.getUserId());
        long likeCount = likeService.count(RelatedType.REPO, repo.getId());
        long commentCount = commentService.count(RelatedType.REPO, repo.getId());
        boolean isLiked = likeService.liked(AuthCtx.getUserId(), RelatedType.REPO, repo.getId());
        Long viewerId = AuthCtx.getUserId();
        boolean isAdmin = viewerId != null && AuthCtx.getRole() == Role.ADMIN;
        boolean isRestricted = repo.getVisibility() == Visibility.RESTRICTED
                && !isAdmin
                && (viewerId == null || !viewerId.equals(repo.getUserId()))
                && !repoAccessService.authorize(viewerId, repo.getId());
        return RepoConverter.INSTANCE.toCardVO(
                repo, user, isLiked, likeCount, (int) commentCount,
                isRestricted, coverOrDefault(repo)
        );
    }

    private RepoDetailVO buildDetailVO(Repo repo) {
        User user = userService.getById(repo.getUserId());
        long likeCount = likeService.count(RelatedType.REPO, repo.getId());
        boolean isLiked = likeService.liked(AuthCtx.getUserId(), RelatedType.REPO, repo.getId());
        List<LikerVO> recentLikers = likeService.getLikers(RelatedType.REPO, repo.getId(), 5);
        return RepoConverter.INSTANCE.toDetailVO(
                repo, user, isLiked, likeCount, recentLikers,
                repo.getType() == RepoType.CODE ? gitHubService.fetch(repo.getUrl()) : null,
                isDefaultCover(repo), coverOrDefault(repo)
        );
    }
}
