package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.UserPermissionMapper;
import com.zincoid.me.model.enums.Perm;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.po.UserPermission;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserPermissionVO;
import com.zincoid.me.service.UserPermissionService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.AuthCtx;
import com.zincoid.me.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionServiceImpl extends ServiceImpl<UserPermissionMapper, UserPermission> implements UserPermissionService {

    private final UserService userService;

    @Override
    public boolean has(Long userId, Perm perm) {
        if (userId == null || perm == null) return false;
        return lambdaQuery()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPerm, perm)
                .exists();
    }

    @Override
    public void require(Perm perm) {
        if (AuthCtx.getRole() == Role.ADMIN) return;
        if (!has(AuthCtx.getUserId(), perm))
            throw new BusinessException(403, "Permission required: " + perm);
    }

    @Override
    @Transactional
    public void grant(Long userId, Perm perm, Long operatorId) {
        User user = userService.getById(userId);
        if (user == null)
            throw new BusinessException(404, "User not found");
        if (has(userId, perm))
            throw new BusinessException(400, "Permission already granted");
        save(UserPermission.builder()
                .userId(userId)
                .perm(perm)
                .grantedBy(operatorId)
                .build());
        log.info("Permission granted: user={}, perm={}, by={}", userId, perm, operatorId);
    }

    @Override
    @Transactional
    public void revoke(Long id, Long operatorId) {
        UserPermission existing = getById(id);
        if (existing == null)
            throw new BusinessException(404, "Permission not found");
        removeById(id);
        log.info("Permission revoked: id={}, user={}, perm={}, by={}", id, existing.getUserId(), existing.getPerm(), operatorId);
    }

    @Override
    public List<Perm> get(Long userId) {
        return lambdaQuery()
                .eq(UserPermission::getUserId, userId)
                .list()
                .stream().map(UserPermission::getPerm).toList();
    }

    @Override
    public PageVO<UserPermissionVO> list(Long userId, Perm perm, int page, int size) {
        Page<UserPermission> p = lambdaQuery()
                .eq(userId != null, UserPermission::getUserId, userId)
                .eq(perm != null, UserPermission::getPerm, perm)
                .orderByDesc(UserPermission::getCreatedAt)
                .page(Page.of(page, size));
        List<UserPermissionVO> vos = new ArrayList<>();
        for (UserPermission up : p.getRecords()) {
            User user = userService.getById(up.getUserId());
            User granter = up.getGrantedBy() != null ? userService.getById(up.getGrantedBy()) : null;
            vos.add(UserPermissionVO.builder()
                    .id(up.getId())
                    .userId(up.getUserId())
                    .username(user != null ? user.getUsername() : null)
                    .userAvatar(user != null ? FileUtil.toThumbUrl(user.getAvatar()) : null)
                    .perm(up.getPerm())
                    .grantedBy(up.getGrantedBy())
                    .grantedByUsername(granter != null ? granter.getUsername() : null)
                    .createdAt(up.getCreatedAt())
                    .build());
        }
        return PageVO.of(p, vos);
    }
}
