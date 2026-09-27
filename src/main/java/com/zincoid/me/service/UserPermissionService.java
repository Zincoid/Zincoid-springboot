package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.Perm;
import com.zincoid.me.model.po.UserPermission;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.UserPermissionVO;

import java.util.List;

public interface UserPermissionService extends IService<UserPermission> {

    boolean has(Long userId, Perm perm);

    void require(Perm perm);

    void grant(Long userId, Perm perm, Long operatorId);

    void revoke(Long userId, Perm perm, Long operatorId);

    List<Perm> get(Long userId);

    PageVO<UserPermissionVO> list(Long userId, Perm perm, int page, int size);
}
