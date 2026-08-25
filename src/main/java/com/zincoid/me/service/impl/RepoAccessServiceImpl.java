package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.mapper.RepoAccessMapper;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.vo.RepoAccessVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RepoAccessServiceImpl extends ServiceImpl<RepoAccessMapper, RepoAccess> implements RepoAccessService {

    private final RepoService repoService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserService userService;

    public RepoAccessServiceImpl(@Lazy RepoService repoService,
                                 NotificationService notificationService,
                                 EmailService emailService,
                                 UserService userService) {
        this.repoService = repoService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.userService = userService;
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
        Repo repo = verifyOwner(ownerId, access);
        access.setAccess(Access.APPROVED);
        updateById(access);
        notificationService.notify(ownerId, access.getUserId(), NotificationType.ACCESS_APPROVED, access.getRepoId());
        emailService.sendAccessApproved(access.getUserId(), repo.getName());
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
    public PageVO<RepoAccessVO> sentPending(Long userId, int page, int size) {
        Page<RepoAccess> p = lambdaQuery().eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getAccess, Access.PENDING).orderByDesc(RepoAccess::getCreatedAt)
                .page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> sentResolved(Long userId, int page, int size) {
        Page<RepoAccess> p = lambdaQuery().eq(RepoAccess::getUserId, userId)
                .ne(RepoAccess::getAccess, Access.PENDING).orderByDesc(RepoAccess::getUpdatedAt)
                .page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> receivedPending(Long ownerId, int page, int size) {
        List<Long> ids = repoService.lambdaQuery().eq(Repo::getUserId, ownerId).select(Repo::getId).list()
                .stream().map(Repo::getId).toList();
        Page<RepoAccess> p = ids.isEmpty() ? Page.of(page, size) : lambdaQuery()
                .in(RepoAccess::getRepoId, ids).eq(RepoAccess::getAccess, Access.PENDING)
                .orderByDesc(RepoAccess::getCreatedAt).page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> receivedResolved(Long ownerId, int page, int size) {
        List<Long> ids = repoService.lambdaQuery().eq(Repo::getUserId, ownerId).select(Repo::getId).list()
                .stream().map(Repo::getId).toList();
        Page<RepoAccess> p = ids.isEmpty() ? Page.of(page, size) : lambdaQuery()
                .in(RepoAccess::getRepoId, ids).ne(RepoAccess::getAccess, Access.PENDING)
                .orderByDesc(RepoAccess::getUpdatedAt).page(Page.of(page, size));
        return toVO(p);
    }

    // ──────── Private tool ────────────────────────────────

    private PageVO<RepoAccessVO> toVO(Page<RepoAccess> page) {
        List<RepoAccessVO> vos = new ArrayList<>();
        for (RepoAccess a : page.getRecords()) {
            Repo repo = repoService.getById(a.getRepoId());
            User user = userService.getById(a.getUserId());
            vos.add(RepoAccessVO.builder()
                    .id(a.getId())
                    .repoId(a.getRepoId())
                    .repoName(repo != null ? repo.getName() : null)
                    .userId(a.getUserId())
                    .userNickname(user != null ? user.getNickname() : null)
                    .userAvatar(user != null ? user.getAvatar() : null)
                    .access(a.getAccess())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build());
        }
        return PageVO.of(page, vos);
    }

    private RepoAccess getOrThrow(Long id) {
        RepoAccess a = getById(id);
        if (a == null) throw new BusinessException(404, "Access record not found");
        return a;
    }

    private Repo verifyOwner(Long ownerId, RepoAccess access) {
        Repo repo = repoService.getById(access.getRepoId());
        if (repo == null || !repo.getUserId().equals(ownerId))
            throw new BusinessException(403, "Only repo owner can manage access");
        return repo;
    }
}
