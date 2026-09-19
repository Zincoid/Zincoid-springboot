package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.mapper.RepoAccessMapper;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.AccessRole;
import com.zincoid.me.model.enums.Visibility;
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
import com.zincoid.me.utils.FileUtil;
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
    public void view(Long userId, Long repoId) {
        Repo repo = repoService.getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (repo.getUserId().equals(userId))
            throw new BusinessException(400, "You are the owner");
        if (repo.getVisibility() != Visibility.RESTRICTED)
            throw new BusinessException(403, "Repo is not restricted");
        RepoAccess existing = lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getRole, AccessRole.VIEWER)
                .one();
        if (existing != null && existing.getAccess() == Access.PENDING)
            throw new BusinessException(400, "Access already requested");
        if (existing != null && existing.getAccess() == Access.APPROVED)
            throw new BusinessException(400, "Access already approved");
        if (existing != null && existing.getAccess() == Access.REJECTED)
            throw new BusinessException(400, "Access already rejected");
        RepoAccess access = RepoAccess.builder()
                .repoId(repoId)
                .userId(userId)
                .role(AccessRole.VIEWER)
                .access(Access.PENDING)
                .build();
        save(access);
        notificationService.notify(userId, repo.getUserId(), NotificationType.REPO_VIEWER_ACCESS_PENDING, repoId);
        log.info("Viewer requested: id={}, user={}, repo={}", access.getId(), userId, repoId);
    }

    @Override
    @Transactional
    public void contribute(Long userId, Long repoId) {
        Repo repo = repoService.getById(repoId);
        if (repo == null)
            throw new BusinessException(404, "Repo not found");
        if (repo.getUserId().equals(userId))
            throw new BusinessException(400, "You are the owner");
        if (repo.getVisibility() == Visibility.PRIVATE)
            throw new BusinessException(403, "Repo is private");
        if (repo.getVisibility() == Visibility.RESTRICTED && !authorize(userId, repoId, AccessRole.VIEWER))
            throw new BusinessException(403, "Repo is restricted and you need viewer access first");
        RepoAccess existing = lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getRole, AccessRole.CONTRIBUTOR)
                .one();
        if (existing != null && existing.getAccess() == Access.PENDING)
            throw new BusinessException(400, "Access already requested");
        if (existing != null && existing.getAccess() == Access.APPROVED)
            throw new BusinessException(400, "Access already approved");
        if (existing != null && existing.getAccess() == Access.REJECTED)
            throw new BusinessException(400, "Access already rejected");
        RepoAccess access = RepoAccess.builder()
                .repoId(repoId)
                .userId(userId)
                .role(AccessRole.CONTRIBUTOR)
                .access(Access.PENDING)
                .build();
        save(access);
        notificationService.notify(userId, repo.getUserId(), NotificationType.REPO_CONTRIBUTOR_ACCESS_PENDING, repoId);
        log.info("Contributor requested: id={}, user={}, repo={}", access.getId(), userId, repoId);
    }

    @Override
    @Transactional
    public void leave(Long userId, Long repoId) {
        RepoAccess existing = lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getRole, AccessRole.CONTRIBUTOR)
                .one();
        if (existing == null || existing.getAccess() != Access.APPROVED)
            throw new BusinessException(404, "You are not a contributor");
        removeById(existing.getId());
        log.info("Contributor left: user={}, repo={}", userId, repoId);
    }

    @Override
    @Transactional
    public void approve(Long userId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        if (access.getAccess() != Access.PENDING)
            throw new BusinessException(400, "Access already resolved");
        Repo repo = verifyOwner(userId, access);
        access.setAccess(Access.APPROVED);
        updateById(access);
        notificationService.notify(userId, access.getUserId(), access.getRole() == AccessRole.CONTRIBUTOR
                ? NotificationType.REPO_CONTRIBUTOR_ACCESS_APPROVED : NotificationType.REPO_VIEWER_ACCESS_APPROVED, access.getRepoId());
        emailService.sendAccessApproved(access.getUserId(), repo.getName(), access.getRole());
        log.info("Access approved: id={}, user={}, repo={}, role={}", accessId, access.getUserId(), access.getRepoId(), access.getRole());
    }

    @Override
    @Transactional
    public void reject(Long userId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        if (access.getAccess() != Access.PENDING)
            throw new BusinessException(400, "Access already resolved");
        verifyOwner(userId, access);
        access.setAccess(Access.REJECTED);
        updateById(access);
        notificationService.notify(userId, access.getUserId(), access.getRole() == AccessRole.CONTRIBUTOR
                ? NotificationType.REPO_CONTRIBUTOR_ACCESS_REJECTED : NotificationType.REPO_VIEWER_ACCESS_REJECTED, access.getRepoId());
        log.info("Access rejected: id={}, user={}, repo={}, role={}", accessId, access.getUserId(), access.getRepoId(), access.getRole());
    }

    @Override
    @Transactional
    public void remove(Long userId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        verifyOwner(userId, access);
        removeById(accessId);
        log.info("Access removed: id={}, user={}, repo={}, role={}", accessId, access.getUserId(), access.getRepoId(), access.getRole());
    }

    @Override
    public boolean authorize(Long userId, Long repoId, AccessRole role) {
        if (userId == null) return false;
        return lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getRole, role)
                .eq(RepoAccess::getAccess, Access.APPROVED)
                .exists();
    }

    @Override
    public PageVO<RepoAccessVO> sentPending(Long userId, AccessRole role, int page, int size) {
        Page<RepoAccess> p = lambdaQuery().eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getAccess, Access.PENDING)
                .eq(role != null, RepoAccess::getRole, role)
                .orderByDesc(RepoAccess::getCreatedAt)
                .page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> sentResolved(Long userId, AccessRole role, int page, int size) {
        Page<RepoAccess> p = lambdaQuery().eq(RepoAccess::getUserId, userId)
                .ne(RepoAccess::getAccess, Access.PENDING)
                .eq(role != null, RepoAccess::getRole, role)
                .orderByDesc(RepoAccess::getUpdatedAt)
                .page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> receivedPending(Long userId, AccessRole role, int page, int size) {
        List<Long> ids = repoService.lambdaQuery().eq(Repo::getUserId, userId).select(Repo::getId).list()
                .stream().map(Repo::getId).toList();
        Page<RepoAccess> p = ids.isEmpty() ? Page.of(page, size) : lambdaQuery()
                .in(RepoAccess::getRepoId, ids).eq(RepoAccess::getAccess, Access.PENDING)
                .eq(role != null, RepoAccess::getRole, role)
                .orderByDesc(RepoAccess::getCreatedAt).page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> receivedResolved(Long userId, AccessRole role, int page, int size) {
        List<Long> ids = repoService.lambdaQuery().eq(Repo::getUserId, userId).select(Repo::getId).list()
                .stream().map(Repo::getId).toList();
        Page<RepoAccess> p = ids.isEmpty() ? Page.of(page, size) : lambdaQuery()
                .in(RepoAccess::getRepoId, ids).ne(RepoAccess::getAccess, Access.PENDING)
                .eq(role != null, RepoAccess::getRole, role)
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
                    .userAvatar(user != null ? FileUtil.toThumbUrl(user.getAvatar()) : null)
                    .role(a.getRole())
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
