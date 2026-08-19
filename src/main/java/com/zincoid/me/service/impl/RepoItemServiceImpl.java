package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.converter.RepoConverter;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.RepoItemMapper;
import com.zincoid.me.model.enums.FileType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.po.RepoItem;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoItemVO;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.RepoItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepoItemServiceImpl extends ServiceImpl<RepoItemMapper, RepoItem> implements RepoItemService {

    private final FileService fileService;

    @Override
    public PageVO<RepoItemVO> list(Long repoId, int page, int size) {
        return PageVO.of(
                lambdaQuery()
                        .eq(RepoItem::getRepoId, repoId)
                        .eq(RepoItem::getStatus, Status.ACTIVE)
                        .orderByAsc(RepoItem::getSortOrder)
                        .page(Page.of(page, size)),
                this::buildItemVO
        );
    }

    @Override
    @Transactional
    public RepoItemVO add(Long repoId, Long fileId, String name) {
        RepoItem item = RepoItem.builder()
                .repoId(repoId)
                .fileId(fileId)
                .name(name)
                .sortOrder(getMaxSortOrder(repoId) + 1)
                .status(Status.ACTIVE)
                .build();
        save(item);
        fileService._link(List.of(fileId), RelatedType.REPO, repoId);
        log.info("Repo item added: repo={}, item={}", repoId, item.getId());
        return buildItemVO(item);
    }

    @Override
    @Transactional
    public void delete(Long itemId) {
        RepoItem item = getById(itemId);
        if (item == null) return;
        if (item.getFileId() != null)
            fileService.delete(item.getFileId());
        removeById(itemId);
        log.info("Repo item deleted: id={}", itemId);
    }

    @Override
    @Transactional
    public void deleteByRepoId(Long repoId) {
        List<RepoItem> items = lambdaQuery()
                .eq(RepoItem::getRepoId, repoId)
                .list();
        fileService.delete(RelatedType.REPO, repoId);
        removeBatchByIds(items.stream().map(RepoItem::getId).toList());
        log.info("Repo items deleted: repo={}, count={}", repoId, items.size());
    }

    @Override
    @Transactional
    public void swap(Long repoId, Long itemIdA, Long itemIdB) {
        RepoItem itemA = getById(itemIdA);
        RepoItem itemB = getById(itemIdB);
        if (itemA == null || itemB == null
                || !itemA.getRepoId().equals(repoId) || !itemB.getRepoId().equals(repoId))
            throw new BusinessException(400, "Invalid Item Sort");
        int orderA = itemA.getSortOrder();
        itemA.setSortOrder(itemB.getSortOrder());
        itemB.setSortOrder(orderA);
        updateById(itemA);
        updateById(itemB);
        log.info("Repo items swapped: repo={}, a={}, b={}", repoId, itemIdA, itemIdB);
    }

    @Override
    public String firstImageUrl(Long repoId) {
        return lambdaQuery()
                .eq(RepoItem::getRepoId, repoId)
                .eq(RepoItem::getStatus, Status.ACTIVE)
                .orderByAsc(RepoItem::getSortOrder)
                .list().stream()
                .filter(i -> {
                    File f = fileService.getById(i.getFileId());
                    return f != null && f.getFileType() == FileType.IMAGE;
                })
                .findFirst()
                .map(i -> "/uploads/" + fileService.getById(i.getFileId()).getFilePath())
                .orElse(null);
    }

    // ──────── Private tool ────────────────────────────────

    private RepoItemVO buildItemVO(RepoItem item) {
        return RepoConverter.INSTANCE.toItemVO(item, fileService.getById(item.getFileId()));
    }

    private int getMaxSortOrder(Long repoId) {
        RepoItem max = lambdaQuery()
                .eq(RepoItem::getRepoId, repoId)
                .orderByDesc(RepoItem::getSortOrder)
                .last("LIMIT 1")
                .one();
        return max == null ? 0 : max.getSortOrder();
    }
}
