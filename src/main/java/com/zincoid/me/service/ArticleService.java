package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.dto.ArticleCreateRequest;
import com.zincoid.me.model.dto.ArticleUpdateRequest;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.vo.ArticleCardVO;
import com.zincoid.me.model.vo.ArticleDetailVO;
import com.zincoid.me.model.vo.PageVO;

import java.util.List;

public interface ArticleService extends IService<Article> {

    ArticleDetailVO create(Long userId, ArticleCreateRequest request);

    ArticleDetailVO update(Long userId, Long articleId, ArticleUpdateRequest request);

    void delete(Long userId, Long articleId, boolean isAdmin);

    void pin(Long articleId);

    void unpin(Long articleId);

    PageVO<ArticleCardVO> list(int page, int size, boolean pinned);

    PageVO<ArticleCardVO> list(Long userId, int page, int size, boolean pinned);

    List<ArticleCardVO> home(int size);

    ArticleDetailVO get(Long articleId);

    ArticleCardVO random();
}
