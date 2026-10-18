package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.type.PermissionType;
import com.zincoid.me.model.po.UserPermission;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserPermissionVO;

import java.util.List;

public interface UserPermissionService extends IService<UserPermission> {

    boolean has(Long userId, PermissionType type);

    void require(PermissionType type);

    void grant(Long userId, PermissionType type, Long operatorId);

    void revoke(Long id, Long operatorId);

    List<PermissionType> get(Long userId);

    PageVO<UserPermissionVO> list(Long userId, PermissionType type, int page, int size);
}
