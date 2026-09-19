package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.AccessRole;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoAccessVO;

public interface RepoAccessService extends IService<RepoAccess> {

    void view(Long userId, Long repoId);

    void contribute(Long userId, Long repoId);

    void leave(Long userId, Long repoId);

    void approve(Long userId, Long accessId);

    void reject(Long userId, Long accessId);

    void remove(Long userId, Long accessId);

    boolean authorize(Long userId, Long repoId, AccessRole role);

    PageVO<RepoAccessVO> sentPending(Long userId, AccessRole role, int page, int size);

    PageVO<RepoAccessVO> sentResolved(Long userId, AccessRole role, int page, int size);

    PageVO<RepoAccessVO> receivedPending(Long userId, AccessRole role, int page, int size);

    PageVO<RepoAccessVO> receivedResolved(Long userId, AccessRole role, int page, int size);
}
