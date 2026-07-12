package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.vo.PageVO;

public interface RepoAccessService extends IService<RepoAccess> {

    void request(Long userId, Long repoId);

    void approve(Long ownerId, Long accessId);

    void reject(Long ownerId, Long accessId);

    void remove(Long ownerId, Long accessId);

    boolean authorize(Long userId, Long repoId);

    PageVO<RepoAccess> sentPending(Long userId, int page, int size);

    PageVO<RepoAccess> sentResolved(Long userId, int page, int size);

    PageVO<RepoAccess> receivedPending(Long ownerId, int page, int size);

    PageVO<RepoAccess> receivedResolved(Long ownerId, int page, int size);
}
