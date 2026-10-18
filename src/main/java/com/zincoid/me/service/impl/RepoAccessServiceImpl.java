package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.Error;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.mapper.RepoAccessMapper;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.AccessType;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.enums.Visibility;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.ContributorVO;
import com.zincoid.me.model.vo.RepoAccessVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import com.zincoid.me.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            throw new BusinessException(Error.REPO_NOT_FOUND);
        if (repo.getUserId().equals(userId))
            throw new BusinessException(Error.YOU_ARE_THE_OWNER);
        if (repo.getVisibility() != Visibility.RESTRICTED)
            throw new BusinessException(Error.REPO_NOT_RESTRICTED);
        RepoAccess existing = lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getType, AccessType.VIEWER)
                .one();
        if (existing != null && existing.getAccess() == Access.PENDING)
            throw new BusinessException(Error.ACCESS_ALREADY_REQUESTED);
        if (existing != null && existing.getAccess() == Access.APPROVED)
            throw new BusinessException(Error.ACCESS_ALREADY_APPROVED);
        if (existing != null && existing.getAccess() == Access.REJECTED)
            throw new BusinessException(Error.ACCESS_ALREADY_REJECTED);
        RepoAccess access = RepoAccess.builder()
                .repoId(repoId)
                .userId(userId)
                .type(AccessType.VIEWER)
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
            throw new BusinessException(Error.REPO_NOT_FOUND);
        if (repo.getType() == RepoType.CODE)
            throw new BusinessException(Error.REPO_IS_CODE_TYPE);
        if (repo.getUserId().equals(userId))
            throw new BusinessException(Error.YOU_ARE_THE_OWNER);
        if (repo.getVisibility() == Visibility.PRIVATE)
            throw new BusinessException(Error.REPO_IS_PRIVATE);
        if (repo.getVisibility() == Visibility.RESTRICTED && !authorize(userId, repoId, AccessType.VIEWER))
            throw new BusinessException(Error.REPO_RESTRICTED_NEED_VIEWER);
        RepoAccess existing = lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getType, AccessType.CONTRIBUTOR)
                .one();
        if (existing != null && existing.getAccess() == Access.PENDING)
            throw new BusinessException(Error.ACCESS_ALREADY_REQUESTED);
        if (existing != null && existing.getAccess() == Access.APPROVED)
            throw new BusinessException(Error.ACCESS_ALREADY_APPROVED);
        if (existing != null && existing.getAccess() == Access.REJECTED)
            throw new BusinessException(Error.ACCESS_ALREADY_REJECTED);
        RepoAccess access = RepoAccess.builder()
                .repoId(repoId)
                .userId(userId)
                .type(AccessType.CONTRIBUTOR)
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
                .eq(RepoAccess::getType, AccessType.CONTRIBUTOR)
                .one();
        if (existing == null || existing.getAccess() != Access.APPROVED)
            throw new BusinessException(Error.NOT_A_CONTRIBUTOR);
        removeById(existing.getId());
        log.info("Contributor left: user={}, repo={}", userId, repoId);
    }

    @Override
    @Transactional
    public void approve(Long userId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        if (access.getAccess() != Access.PENDING)
            throw new BusinessException(Error.ACCESS_ALREADY_RESOLVED);
        Repo repo = verifyOwner(userId, access);
        access.setAccess(Access.APPROVED);
        updateById(access);
        notificationService.notify(userId, access.getUserId(), access.getType() == AccessType.CONTRIBUTOR
                ? NotificationType.REPO_CONTRIBUTOR_ACCESS_APPROVED : NotificationType.REPO_VIEWER_ACCESS_APPROVED, access.getRepoId());
        emailService.sendAccessApproved(access.getUserId(), repo.getName(), access.getType());
        log.info("Access approved: id={}, user={}, repo={}, type={}", accessId, access.getUserId(), access.getRepoId(), access.getType());
    }

    @Override
    @Transactional
    public void reject(Long userId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        if (access.getAccess() != Access.PENDING)
            throw new BusinessException(Error.ACCESS_ALREADY_RESOLVED);
        verifyOwner(userId, access);
        access.setAccess(Access.REJECTED);
        updateById(access);
        notificationService.notify(userId, access.getUserId(), access.getType() == AccessType.CONTRIBUTOR
                ? NotificationType.REPO_CONTRIBUTOR_ACCESS_REJECTED : NotificationType.REPO_VIEWER_ACCESS_REJECTED, access.getRepoId());
        log.info("Access rejected: id={}, user={}, repo={}, type={}", accessId, access.getUserId(), access.getRepoId(), access.getType());
    }

    @Override
    @Transactional
    public void remove(Long userId, Long accessId) {
        RepoAccess access = getOrThrow(accessId);
        verifyOwner(userId, access);
        removeById(accessId);
        if (access.getType() == AccessType.VIEWER)
            lambdaUpdate()
                    .eq(RepoAccess::getRepoId, access.getRepoId())
                    .eq(RepoAccess::getUserId, access.getUserId())
                    .eq(RepoAccess::getType, AccessType.CONTRIBUTOR)
                    .remove();
        log.info("Access removed: id={}, user={}, repo={}, type={}", accessId, access.getUserId(), access.getRepoId(), access.getType());
    }

    @Override
    public boolean authorize(Long userId, Long repoId, AccessType type) {
        if (userId == null) return false;
        return lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getType, type)
                .eq(RepoAccess::getAccess, Access.APPROVED)
                .exists();
    }

    @Override
    public PageVO<RepoAccessVO> sentPending(Long userId, AccessType type, int page, int size) {
        Page<RepoAccess> p = lambdaQuery().eq(RepoAccess::getUserId, userId)
                .eq(RepoAccess::getAccess, Access.PENDING)
                .eq(type != null, RepoAccess::getType, type)
                .orderByDesc(RepoAccess::getCreatedAt)
                .page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> sentResolved(Long userId, AccessType type, int page, int size) {
        Page<RepoAccess> p = lambdaQuery().eq(RepoAccess::getUserId, userId)
                .ne(RepoAccess::getAccess, Access.PENDING)
                .eq(type != null, RepoAccess::getType, type)
                .orderByDesc(RepoAccess::getUpdatedAt)
                .page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> receivedPending(Long userId, Long repoId, AccessType type, int page, int size) {
        List<Long> ids = repoService.lambdaQuery()
                .eq(repoId != null, Repo::getId, repoId)
                .eq(Repo::getUserId, userId)
                .select(Repo::getId)
                .list()
                .stream().map(Repo::getId).toList();
        Page<RepoAccess> p = ids.isEmpty() ? Page.of(page, size) : lambdaQuery()
                .in(RepoAccess::getRepoId, ids).eq(RepoAccess::getAccess, Access.PENDING)
                .eq(type != null, RepoAccess::getType, type)
                .orderByDesc(RepoAccess::getCreatedAt).page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public PageVO<RepoAccessVO> receivedResolved(Long userId, Long repoId, AccessType type, int page, int size) {
        List<Long> ids = repoService.lambdaQuery()
                .eq(repoId != null, Repo::getId, repoId)
                .eq(Repo::getUserId, userId)
                .select(Repo::getId)
                .list()
                .stream().map(Repo::getId).toList();
        Page<RepoAccess> p = ids.isEmpty() ? Page.of(page, size) : lambdaQuery()
                .in(RepoAccess::getRepoId, ids).ne(RepoAccess::getAccess, Access.PENDING)
                .eq(type != null, RepoAccess::getType, type)
                .orderByDesc(RepoAccess::getUpdatedAt).page(Page.of(page, size));
        return toVO(p);
    }

    @Override
    public long countContributors(Long repoId) {
        return lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getType, AccessType.CONTRIBUTOR)
                .eq(RepoAccess::getAccess, Access.APPROVED)
                .count();
    }

    @Override
    public List<ContributorVO> recentContributors(Long repoId, int limit) {
        Repo repo = repoService.getById(repoId);
        if (repo == null || repo.getStatus() == Status.DISABLED)
            throw new BusinessException(Error.REPO_NOT_FOUND);
        Long viewerId = AuthCtx.getUserId();
        boolean isAdmin = viewerId != null && AuthCtx.getRole() == Role.ADMIN;
        if (repo.getVisibility() == Visibility.PRIVATE && !isAdmin
                && (viewerId == null || !viewerId.equals(repo.getUserId())))
            throw new BusinessException(Error.REPO_IS_PRIVATE);
        return lambdaQuery()
                .eq(RepoAccess::getRepoId, repoId)
                .eq(RepoAccess::getType, AccessType.CONTRIBUTOR)
                .eq(RepoAccess::getAccess, Access.APPROVED)
                .orderByDesc(RepoAccess::getUpdatedAt)
                .page(Page.of(1, limit))
                .getRecords()
                .stream().map(a -> {
                    User user = userService.getById(a.getUserId());
                    if (user == null) return null;
                    return ContributorVO.builder()
                            .userId(user.getId())
                            .nickname(user.getNickname())
                            .avatar(FileUtil.toThumbUrl(user.getAvatar()))
                            .build();
                }).filter(Objects::nonNull).toList();
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
                    .type(a.getType())
                    .access(a.getAccess())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build());
        }
        return PageVO.of(page, vos);
    }

    private RepoAccess getOrThrow(Long id) {
        RepoAccess a = getById(id);
        if (a == null) throw new BusinessException(Error.ACCESS_RECORD_NOT_FOUND);
        return a;
    }

    private Repo verifyOwner(Long ownerId, RepoAccess access) {
        Repo repo = repoService.getById(access.getRepoId());
        if (repo == null || !repo.getUserId().equals(ownerId))
            throw new BusinessException(Error.ONLY_OWNER_MANAGE_ACCESS);
        return repo;
    }
}
