package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.dto.ArticleCreateRequest;
import com.zincoid.me.model.dto.ArticleUpdateRequest;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.ArticleCardVO;
import com.zincoid.me.model.vo.ArticleDetailVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // ──── Private endpoints ───────────────

    @PostMapping
    public ApiResp<ArticleDetailVO> createArticle(@Valid @RequestBody ArticleCreateRequest request) {
        return ApiResp.success(articleService.create(AuthCtx.getUserId(), request));
    }

    @PutMapping("/{articleId}")
    public ApiResp<ArticleDetailVO> updateArticle(@PathVariable Long articleId,
                                                  @RequestBody ArticleUpdateRequest request) {
        return ApiResp.success(articleService.update(AuthCtx.getUserId(), articleId, request));
    }

    @DeleteMapping("/{articleId}")
    public ApiResp<Void> deleteArticle(@PathVariable Long articleId) {
        articleService.delete(AuthCtx.getUserId(), articleId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResp.success();
    }

    @PutMapping("/{articleId}/pin")
    public ApiResp<Void> pinArticle(@PathVariable Long articleId) {
        AuthCtx.requireAdmin();
        articleService.pin(articleId);
        return ApiResp.success();
    }

    @PutMapping("/{articleId}/unpin")
    public ApiResp<Void> unpinArticle(@PathVariable Long articleId) {
        AuthCtx.requireAdmin();
        articleService.unpin(articleId);
        return ApiResp.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/random")
    public ApiResp<ArticleCardVO> randomArticle() {
        return ApiResp.success(articleService.random());
    }

    @GetMapping("/public/home")
    public ApiResp<List<ArticleCardVO>> homeArticles(@RequestParam(defaultValue = "10") int size) {
        return ApiResp.success(articleService.home(size));
    }

    @GetMapping("/public")
    public ApiResp<PageVO<ArticleCardVO>> listArticles(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResp.success(articleService.list(page, size, pinned));
    }

    @GetMapping("/public/user/{userId}")
    public ApiResp<PageVO<ArticleCardVO>> userArticles(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResp.success(articleService.list(userId, page, size, pinned));
    }

    @GetMapping("/public/{articleId}")
    public ApiResp<ArticleDetailVO> articleDetail(@PathVariable Long articleId) {
        return ApiResp.success(articleService.get(articleId));
    }
}
