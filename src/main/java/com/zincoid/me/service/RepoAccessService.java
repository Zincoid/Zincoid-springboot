package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoAccessVO;

public interface RepoAccessService extends IService<RepoAccess> {

    void request(Long userId, Long repoId);

    void approve(Long ownerId, Long accessId);

    void reject(Long ownerId, Long accessId);

    void remove(Long ownerId, Long accessId);

    boolean authorize(Long userId, Long repoId);

    PageVO<RepoAccessVO> sentPending(Long userId, int page, int size);

    PageVO<RepoAccessVO> sentResolved(Long userId, int page, int size);

    PageVO<RepoAccessVO> receivedPending(Long ownerId, int page, int size);

    PageVO<RepoAccessVO> receivedResolved(Long ownerId, int page, int size);
}
