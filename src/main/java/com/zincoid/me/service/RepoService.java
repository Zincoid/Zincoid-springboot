package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.dto.RepoCreateRequest;
import com.zincoid.me.model.dto.RepoUpdateRequest;
import com.zincoid.me.model.enums.RepoType;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoCardVO;
import com.zincoid.me.model.vo.RepoDetailVO;
import com.zincoid.me.model.vo.RepoItemVO;

import java.util.List;

public interface RepoService extends IService<Repo> {

    RepoDetailVO create(Long userId, RepoCreateRequest request);

    RepoDetailVO update(Long userId, Long repoId, RepoUpdateRequest request);

    void delete(Long userId, Long repoId, boolean isAdmin);

    void pin(Long repoId);

    void unpin(Long repoId);

    RepoItemVO addItem(Long userId, Long repoId, Long fileId);

    void deleteItem(Long userId, Long repoId, Long itemId);

    void swapItems(Long userId, Long repoId, Long itemIdA, Long itemIdB);

    PageVO<RepoCardVO> list(RepoType type, String keyword, boolean tagged, boolean updated, boolean pinned, int page, int size);

    PageVO<RepoCardVO> list(RepoType type, Long userId, boolean updated, boolean pinned, int page, int size);

    List<RepoCardVO> home(int size);

    RepoCardVO random();

    RepoDetailVO get(Long repoId);

    PageVO<RepoItemVO> items(Long repoId, int page, int size);
}
