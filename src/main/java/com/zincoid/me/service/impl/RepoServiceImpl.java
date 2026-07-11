package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.RepoMapper;
import com.zincoid.me.model.dto.RepoCreateRequest;
import com.zincoid.me.model.dto.RepoItemAddRequest;
import com.zincoid.me.model.dto.RepoUpdateRequest;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.enums.Visibility;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.RepoItem;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoCardVO;
import com.zincoid.me.model.vo.RepoDetailVO;
import com.zincoid.me.model.vo.RepoItemVO;
import com.zincoid.me.service.FileService;
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
                .coverImage(request.getCoverImage())
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
        if (request.getVisibility() != null) repo.setVisibility(request.getVisibility());
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
        RepoItem item = repoItemService.add(repoId, request.getFileId(), request.getName());
        return buildItemVO(item);
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
    public void sortItems(Long userId, Long repoId, List<Long> itemIds) {
        Repo repo = getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (!repo.getUserId().equals(userId))
            throw new BusinessException(403, "You can only edit your own repos");
        repoItemService.sortItems(repoId, itemIds);
    }

    @Override
    public PageVO<RepoCardVO> list(RepoType type, String keyword, int page, int size) {
        Page<Repo> repoPage = lambdaQuery()
                .eq(Repo::getStatus, Status.ACTIVE)
                .eq(Repo::getVisibility, Visibility.PUBLIC)
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
        var wrapper = lambdaQuery()
                .eq(Repo::getStatus, Status.ACTIVE)
                .eq(Repo::getUserId, userId)
                .eq(type != null, Repo::getType, type)
                .eq(!isOwner, Repo::getVisibility, Visibility.PUBLIC)
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
        if (repo.getVisibility() == Visibility.PRIVATE
                && (viewerId == null || !viewerId.equals(repo.getUserId())))
            throw new BusinessException(404, "Repo is private");
        return buildDetailVO(repo);
    }

    // ──────── Private tool ────────────────────────────────

    private RepoCardVO buildCardVO(Repo repo) {
        User user = userService.getById(repo.getUserId());
        return RepoCardVO.builder()
                .id(repo.getId())
                .userId(repo.getUserId())
                .userNickname(user != null ? user.getNickname() : null)
                .userAvatar(user != null ? user.getAvatar() : null)
                .name(repo.getName())
                .description(repo.getDescription())
                .type(repo.getType())
                .visibility(repo.getVisibility())
                .url(repo.getUrl())
                .tags(JsonUtil.parseImages(repo.getTags()))
                .coverImage(repo.getCoverImage())
                .createdAt(repo.getCreatedAt())
                .build();
    }

    private RepoDetailVO buildDetailVO(Repo repo) {
        User user = userService.getById(repo.getUserId());
        List<RepoItemVO> items = List.of();
        if (repo.getType() != RepoType.CODE) {
            items = repoItemService.list(repo.getId())
                    .stream().map(this::buildItemVO).toList();
        }
        return RepoDetailVO.builder()
                .id(repo.getId())
                .userId(repo.getUserId())
                .userNickname(user != null ? user.getNickname() : null)
                .userAvatar(user != null ? user.getAvatar() : null)
                .name(repo.getName())
                .description(repo.getDescription())
                .type(repo.getType())
                .visibility(repo.getVisibility())
                .url(repo.getUrl())
                .tags(JsonUtil.parseImages(repo.getTags()))
                .coverImage(repo.getCoverImage())
                .items(items)
                .createdAt(repo.getCreatedAt())
                .updatedAt(repo.getUpdatedAt())
                .build();
    }

    private RepoItemVO buildItemVO(RepoItem item) {
        File file = fileService.getById(item.getFileId());
        return RepoItemVO.builder()
                .id(item.getId())
                .sortOrder(item.getSortOrder())
                .fileId(item.getFileId())
                .name(item.getName())
                .url(file != null ? "/uploads/" + file.getFilePath() : null)
                .fileSize(file != null ? file.getFileSize() : null)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
