package com.zincoid.me.controller;

import com.zincoid.me.model.dto.ArticleCreateRequest;
import com.zincoid.me.model.dto.ArticleUpdateRequest;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.ApiResponse;
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

    // ──── Private endpoints ────────────────

    @PostMapping
    public ApiResponse<ArticleDetailVO> createArticle(@Valid @RequestBody ArticleCreateRequest request) {
        return ApiResponse.success(articleService.create(AuthCtx.getUserId(), request));
    }

    @PutMapping("/{articleId}")
    public ApiResponse<ArticleDetailVO> updateArticle(@PathVariable Long articleId,
                                                      @RequestBody ArticleUpdateRequest request) {
        return ApiResponse.success(articleService.update(AuthCtx.getUserId(), articleId, request));
    }

    @DeleteMapping("/{articleId}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long articleId) {
        articleService.delete(AuthCtx.getUserId(), articleId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }

    @PutMapping("/{articleId}/pin")
    public ApiResponse<Void> pinArticle(@PathVariable Long articleId) {
        AuthCtx.requireAdmin();
        articleService.pin(articleId);
        return ApiResponse.success();
    }

    @PutMapping("/{articleId}/unpin")
    public ApiResponse<Void> unpinArticle(@PathVariable Long articleId) {
        AuthCtx.requireAdmin();
        articleService.unpin(articleId);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/random")
    public ApiResponse<ArticleCardVO> randomArticle() {
        return ApiResponse.success(articleService.random());
    }

    @GetMapping("/public")
    public ApiResponse<PageVO<ArticleCardVO>> listArticles(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResponse.success(articleService.list(page, size, pinned));
    }

    @GetMapping("/public/home")
    public ApiResponse<List<ArticleCardVO>> homeFeed(@RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(articleService.home(size));
    }

    @GetMapping("/public/user/{userId}")
    public ApiResponse<PageVO<ArticleCardVO>> userArticles(@PathVariable Long userId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResponse.success(articleService.list(userId, page, size, pinned));
    }

    @GetMapping("/public/{articleId}")
    public ApiResponse<ArticleDetailVO> articleDetail(@PathVariable Long articleId) {
        return ApiResponse.success(articleService.get(articleId));
    }
}
