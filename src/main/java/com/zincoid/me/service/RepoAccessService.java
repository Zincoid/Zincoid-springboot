package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.AccessType;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.vo.ContributorVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoAccessVO;

import java.util.List;

public interface RepoAccessService extends IService<RepoAccess> {

    void view(Long userId, Long repoId);

    void contribute(Long userId, Long repoId);

    void leave(Long userId, Long repoId);

    void approve(Long userId, Long accessId);

    void reject(Long userId, Long accessId);

    void remove(Long userId, Long accessId);

    boolean authorize(Long userId, Long repoId, AccessType type);

    PageVO<RepoAccessVO> sentPending(Long userId, AccessType type, int page, int size);

    PageVO<RepoAccessVO> sentResolved(Long userId, AccessType type, int page, int size);

    PageVO<RepoAccessVO> receivedPending(Long userId, Long repoId, AccessType type, int page, int size);

    PageVO<RepoAccessVO> receivedResolved(Long userId, Long repoId, AccessType type, int page, int size);

    long countContributors(Long repoId);

    List<ContributorVO> recentContributors(Long repoId, int limit);
}
