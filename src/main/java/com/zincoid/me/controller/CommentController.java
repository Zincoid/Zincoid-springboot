package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.dto.CommentCreateRequest;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.CommentCardVO;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.model.vo.PageVO;

import java.util.List;

import com.zincoid.me.service.CommentService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ──── Private endpoints ───────────────

    @PostMapping("/moment/{momentId}")
    public ApiResp<CommentVO> addMomentComment(@PathVariable Long momentId,
                                               @Valid @RequestBody CommentCreateRequest request) {
        return ApiResp.success(commentService.add(AuthCtx.getUserId(),
                RelatedType.MOMENT, momentId, request.getContent(), request.getParentId()));
    }

    @PostMapping("/article/{articleId}")
    public ApiResp<CommentVO> addArticleComment(@PathVariable Long articleId,
                                                @Valid @RequestBody CommentCreateRequest request) {
        return ApiResp.success(commentService.add(AuthCtx.getUserId(),
                RelatedType.ARTICLE, articleId, request.getContent(), request.getParentId()));
    }

    @PostMapping("/repo/{repoId}")
    public ApiResp<CommentVO> addRepoComment(@PathVariable Long repoId,
                                             @Valid @RequestBody CommentCreateRequest request) {
        return ApiResp.success(commentService.add(AuthCtx.getUserId(),
                RelatedType.REPO, repoId, request.getContent(), request.getParentId()));
    }

    @PutMapping("/{commentId}/pin")
    public ApiResp<Void> pinComment(@PathVariable Long commentId) {
        commentService.pin(AuthCtx.getUserId(), commentId);
        return ApiResp.success();
    }

    @PutMapping("/{commentId}/unpin")
    public ApiResp<Void> unpinComment(@PathVariable Long commentId) {
        commentService.unpin(AuthCtx.getUserId(), commentId);
        return ApiResp.success();
    }

    @DeleteMapping("/{commentId}")
    public ApiResp<Void> deleteComment(@PathVariable Long commentId) {
        commentService.delete(AuthCtx.getUserId(), commentId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResp.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/home")
    public ApiResp<List<CommentCardVO>> homeComments(@RequestParam(defaultValue = "10") int size) {
        return ApiResp.success(commentService.home(size));
    }

    @GetMapping("/public/count/{targetType}/{targetId}")
    public ApiResp<Long> countComments(@PathVariable RelatedType targetType, @PathVariable Long targetId) {
        return ApiResp.success(commentService.count(targetType, targetId));
    }

    @GetMapping("/public/moment/{momentId}")
    public ApiResp<PageVO<CommentVO>> momentComments(@PathVariable Long momentId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(defaultValue = "false") boolean hot) {
        return ApiResp.success(commentService.list(RelatedType.MOMENT, momentId, hot, page, size));
    }

    @GetMapping("/public/article/{articleId}")
    public ApiResp<PageVO<CommentVO>> articleComments(@PathVariable Long articleId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(defaultValue = "false") boolean hot) {
        return ApiResp.success(commentService.list(RelatedType.ARTICLE, articleId, hot, page, size));
    }

    @GetMapping("/public/repo/{repoId}")
    public ApiResp<PageVO<CommentVO>> repoComments(@PathVariable Long repoId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue = "false") boolean hot) {
        return ApiResp.success(commentService.list(RelatedType.REPO, repoId, hot, page, size));
    }

    @GetMapping("/public/replies/{parentId}")
    public ApiResp<List<CommentVO>> replies(@PathVariable Long parentId) {
        return ApiResp.success(commentService.replies(parentId));
    }

    @GetMapping("/public/{commentId}")
    public ApiResp<CommentCardVO> card(@PathVariable Long commentId) {
        return ApiResp.success(commentService.card(commentId));
    }
}
