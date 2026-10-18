package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.Error;
import com.zincoid.me.mapper.UserPermissionMapper;
import com.zincoid.me.model.enums.Permission;
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
    public boolean has(Long userId, Permission permission) {
        if (userId == null || permission == null) return false;
        return lambdaQuery()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermission, permission)
                .exists();
    }

    @Override
    public void require(Permission permission) {
        if (AuthCtx.getRole() == Role.ADMIN) return;
        if (!has(AuthCtx.getUserId(), permission)) {
            log.warn("Permission required: {}", permission);
            throw new BusinessException(Error.PERMISSION_REQUIRED);
        }
    }

    @Override
    @Transactional
    public void grant(Long userId, Permission permission, Long operatorId) {
        User user = userService.getById(userId);
        if (user == null)
            throw new BusinessException(Error.USER_NOT_FOUND);
        if (has(userId, permission))
            throw new BusinessException(Error.PERMISSION_ALREADY_GRANTED);
        save(UserPermission.builder()
                .userId(userId)
                .permission(permission)
                .grantedBy(operatorId)
                .build());
        log.info("Permission granted: user={}, permission={}, by={}", userId, permission, operatorId);
    }

    @Override
    @Transactional
    public void revoke(Long id, Long operatorId) {
        UserPermission existing = getById(id);
        if (existing == null)
            throw new BusinessException(Error.PERMISSION_NOT_FOUND);
        removeById(id);
        log.info("Permission revoked: id={}, user={}, permission={}, by={}", id, existing.getUserId(), existing.getPermission(), operatorId);
    }

    @Override
    public List<Permission> get(Long userId) {
        return lambdaQuery()
                .eq(UserPermission::getUserId, userId)
                .list()
                .stream().map(UserPermission::getPermission).toList();
    }

    @Override
    public PageVO<UserPermissionVO> list(Long userId, Permission permission, int page, int size) {
        Page<UserPermission> p = lambdaQuery()
                .eq(userId != null, UserPermission::getUserId, userId)
                .eq(permission != null, UserPermission::getPermission, permission)
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
                    .permission(up.getPermission())
                    .grantedBy(up.getGrantedBy())
                    .grantedByUsername(granter != null ? granter.getUsername() : null)
                    .createdAt(up.getCreatedAt())
                    .build());
        }
        return PageVO.of(p, vos);
    }
}
