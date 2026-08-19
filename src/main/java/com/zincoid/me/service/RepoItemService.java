package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.RepoItem;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoItemVO;

import java.util.List;

public interface RepoItemService extends IService<RepoItem> {

    PageVO<RepoItemVO> list(Long repoId, int page, int size);

    RepoItemVO add(Long repoId, Long fileId, String name);

    void delete(Long itemId);

    void deleteByRepoId(Long repoId);

    void sortItems(Long repoId, List<Long> itemIds);

    String firstImageUrl(Long repoId);
}
