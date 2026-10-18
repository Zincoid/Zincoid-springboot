package com.zincoid.me.controller;

import com.zincoid.me.model.dto.CommentCreateRequest;
import com.zincoid.me.model.enums.type.BizType;
import com.zincoid.me.model.enums.state.Role;
import com.zincoid.me.model.ApiResponse;
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
    public ApiResponse<CommentVO> addMomentComment(@PathVariable Long momentId,
                                                   @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.add(AuthCtx.getUserId(),
                BizType.MOMENT, momentId, request.getContent(), request.getParentId()));
    }

    @PostMapping("/article/{articleId}")
    public ApiResponse<CommentVO> addArticleComment(@PathVariable Long articleId,
                                                    @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.add(AuthCtx.getUserId(),
                BizType.ARTICLE, articleId, request.getContent(), request.getParentId()));
    }

    @PostMapping("/repo/{repoId}")
    public ApiResponse<CommentVO> addRepoComment(@PathVariable Long repoId,
                                                 @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.add(AuthCtx.getUserId(),
                BizType.REPO, repoId, request.getContent(), request.getParentId()));
    }

    @PutMapping("/{commentId}/pin")
    public ApiResponse<Void> pinComment(@PathVariable Long commentId) {
        commentService.pin(AuthCtx.getUserId(), commentId);
        return ApiResponse.success();
    }

    @PutMapping("/{commentId}/unpin")
    public ApiResponse<Void> unpinComment(@PathVariable Long commentId) {
        commentService.unpin(AuthCtx.getUserId(), commentId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        commentService.delete(AuthCtx.getUserId(), commentId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/home")
    public ApiResponse<List<CommentCardVO>> homeComments(@RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(commentService.home(size));
    }

    @GetMapping("/public/count/{targetType}/{targetId}")
    public ApiResponse<Long> countComments(@PathVariable BizType targetType, @PathVariable Long targetId) {
        return ApiResponse.success(commentService.count(targetType, targetId));
    }

    @GetMapping("/public/moment/{momentId}")
    public ApiResponse<PageVO<CommentVO>> momentComments(@PathVariable Long momentId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(defaultValue = "false") boolean hot) {
        return ApiResponse.success(commentService.list(BizType.MOMENT, momentId, hot, page, size));
    }

    @GetMapping("/public/article/{articleId}")
    public ApiResponse<PageVO<CommentVO>> articleComments(@PathVariable Long articleId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(defaultValue = "false") boolean hot) {
        return ApiResponse.success(commentService.list(BizType.ARTICLE, articleId, hot, page, size));
    }

    @GetMapping("/public/repo/{repoId}")
    public ApiResponse<PageVO<CommentVO>> repoComments(@PathVariable Long repoId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "false") boolean hot) {
        return ApiResponse.success(commentService.list(BizType.REPO, repoId, hot, page, size));
    }

    @GetMapping("/public/replies/{parentId}")
    public ApiResponse<List<CommentVO>> replies(@PathVariable Long parentId) {
        return ApiResponse.success(commentService.replies(parentId));
    }

    @GetMapping("/public/{commentId}")
    public ApiResponse<CommentCardVO> card(@PathVariable Long commentId) {
        return ApiResponse.success(commentService.card(commentId));
    }
}
