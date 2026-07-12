package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.RepoAccess;

import java.util.List;

public interface RepoAccessService extends IService<RepoAccess> {

    void request(Long userId, Long repoId);

    void approve(Long ownerId, Long accessId);

    void reject(Long ownerId, Long accessId);

    void remove(Long ownerId, Long accessId);

    boolean authorize(Long userId, Long repoId);

    List<RepoAccess> sentPending(Long userId);

    List<RepoAccess> sentResolved(Long userId);

    List<RepoAccess> receivedPending(Long ownerId);

    List<RepoAccess> receivedResolved(Long ownerId);
}
