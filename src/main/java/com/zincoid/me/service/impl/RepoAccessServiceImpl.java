package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.RepoAccessMapper;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.service.RepoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class RepoAccessServiceImpl extends ServiceImpl<RepoAccessMapper, RepoAccess> implements RepoAccessService {

    private final RepoService repoService;
    private final NotificationService notificationService;

    public RepoAccessServiceImpl(@Lazy RepoService repoService,
                                 NotificationService notificationService) {
        this.repoService = repoService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void request(Long userId, Long repoId) {
        Repo repo = repoService.getById(repoId);
        if (repo == null) throw new BusinessException(404, "Repo not found");
        RepoAccess existing = lambdaQuery().eq(RepoAccess::getRepoId, repoId).eq(RepoAccess::getUserId, userId).one();
        if (existing != null && existing.getAccess() == Access.PENDING)
            throw new BusinessException(400, "Access already requested");
        if (existing != null && existing.getAccess() == Access.APPROVED)
            throw new BusinessException(400, "Access already approved");
        if (existing != null && existing.getAccess() == Access.REJECTED)
            throw new BusinessException(400, "Access already rejected");
        RepoAccess access = RepoAccess.builder().repoId(repoId).userId(userId).access(Access.PENDING).build();
        save(access);
        notificationService.notify(userId, repo.getUserId(), NotificationType.ACCESS_REQUEST, repoId);
        log.info("Access requested: id={}, user={}, repo={}", access.getId(), userId, repoId);
    }

    @Override
    @Transactional
    public void approve(Long ownerId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        verifyOwner(ownerId, access);
        access.setAccess(Access.APPROVED);
        updateById(access);
        notificationService.notify(ownerId, access.getUserId(), NotificationType.ACCESS_APPROVED, access.getRepoId());
        log.info("Access approved: id={}, user={}, repo={}", accessId, access.getUserId(), access.getRepoId());
    }

    @Override
    @Transactional
    public void reject(Long ownerId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        verifyOwner(ownerId, access);
        access.setAccess(Access.REJECTED);
        updateById(access);
        notificationService.notify(ownerId, access.getUserId(), NotificationType.ACCESS_REJECTED, access.getRepoId());
        log.info("Access rejected: id={}, user={}, repo={}", accessId, access.getUserId(), access.getRepoId());
    }

    @Override
    @Transactional
    public void remove(Long ownerId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        verifyOwner(ownerId, access);
        removeById(accessId);
        log.info("Access removed: id={}, user={}, repo={}", accessId, access.getUserId(), access.getRepoId());
    }

    @Override
    public boolean authorize(Long userId, Long repoId) {
        if (userId == null) return false;
        return lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getAccess, Access.APPROVED)
                .exists();
    }

    @Override
    public List<RepoAccess> sentPending(Long userId) {
        return lambdaQuery()
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getAccess, Access.PENDING).list();
    }

    @Override
    public List<RepoAccess> sentResolved(Long userId) {
        return lambdaQuery()
                .eq(RepoAccess::getUserId, userId)
                .ne(RepoAccess::getAccess, Access.PENDING).list();
    }

    @Override
    public List<RepoAccess> receivedPending(Long ownerId) {
        List<Long> repoIds = repoService.lambdaQuery()
                .eq(Repo::getUserId, ownerId).select(Repo::getId).list()
                .stream().map(Repo::getId).toList();
        return repoIds.isEmpty() ? List.of() : lambdaQuery()
                .in(RepoAccess::getRepoId, repoIds)
                .eq(RepoAccess::getAccess, Access.PENDING).list();
    }

    @Override
    public List<RepoAccess> receivedResolved(Long ownerId) {
        List<Long> repoIds = repoService.lambdaQuery()
                .eq(Repo::getUserId, ownerId).select(Repo::getId).list()
                .stream().map(Repo::getId).toList();
        return repoIds.isEmpty() ? List.of() : lambdaQuery()
                .in(RepoAccess::getRepoId, repoIds)
                .ne(RepoAccess::getAccess, Access.PENDING).list();
    }

    // ──────── Private tool ────────────────────────────────

    private RepoAccess getOrThrow(Long id) {
        RepoAccess a = getById(id);
        if (a == null) throw new BusinessException(404, "Access record not found");
        return a;
    }

    private void verifyOwner(Long ownerId, RepoAccess access) {
        Repo repo = repoService.getById(access.getRepoId());
        if (repo == null || !repo.getUserId().equals(ownerId))
            throw new BusinessException(403, "Only repo owner can manage access");
    }
}
