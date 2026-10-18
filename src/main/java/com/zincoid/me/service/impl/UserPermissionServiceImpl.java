package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.dict.Error;
import com.zincoid.me.mapper.UserPermissionMapper;
import com.zincoid.me.model.enums.type.PermissionType;
import com.zincoid.me.model.enums.state.Role;
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
    public boolean has(Long userId, PermissionType type) {
        if (userId == null || type == null) return false;
        return lambdaQuery()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getType, type)
                .exists();
    }

    @Override
    public void require(PermissionType type) {
        if (AuthCtx.getRole() == Role.ADMIN) return;
        if (!has(AuthCtx.getUserId(), type)) {
            log.warn("Permission required: {}", type);
            throw new BusinessException(Error.PERMISSION_REQUIRED);
        }
    }

    @Override
    @Transactional
    public void grant(Long userId, PermissionType type, Long operatorId) {
        User user = userService.getById(userId);
        if (user == null)
            throw new BusinessException(Error.USER_NOT_FOUND);
        if (has(userId, type))
            throw new BusinessException(Error.PERMISSION_ALREADY_GRANTED);
        save(UserPermission.builder()
                .userId(userId)
                .type(type)
                .grantedBy(operatorId)
                .build());
        log.info("Permission granted: user={}, type={}, by={}", userId, type, operatorId);
    }

    @Override
    @Transactional
    public void revoke(Long id, Long operatorId) {
        UserPermission existing = getById(id);
        if (existing == null)
            throw new BusinessException(Error.PERMISSION_NOT_FOUND);
        removeById(id);
        log.info("Permission revoked: id={}, user={}, type={}, by={}", id, existing.getUserId(), existing.getType(), operatorId);
    }

    @Override
    public List<PermissionType> get(Long userId) {
        return lambdaQuery()
                .eq(UserPermission::getUserId, userId)
                .list()
                .stream().map(UserPermission::getType).toList();
    }

    @Override
    public PageVO<UserPermissionVO> list(Long userId, PermissionType type, int page, int size) {
        Page<UserPermission> p = lambdaQuery()
                .eq(userId != null, UserPermission::getUserId, userId)
                .eq(type != null, UserPermission::getType, type)
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
                    .type(up.getType())
                    .grantedBy(up.getGrantedBy())
                    .grantedByUsername(granter != null ? granter.getUsername() : null)
                    .createdAt(up.getCreatedAt())
                    .build());
        }
        return PageVO.of(p, vos);
    }
}
