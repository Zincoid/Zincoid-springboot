package com.zincoid.me.controller;

import com.zincoid.me.model.dto.CommentCreateRequest;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ──── Private endpoints ────────────────

    @PostMapping("/moment/{momentId}")
    public ApiResponse<CommentVO> addMomentComment(@PathVariable Long momentId,
                                                   @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.add(AuthCtx.getUserId(),
                RelatedType.MOMENT, momentId, request.getContent(), request.getParentId()));
    }

    @PostMapping("/article/{articleId}")
    public ApiResponse<CommentVO> addArticleComment(@PathVariable Long articleId,
                                                    @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.add(AuthCtx.getUserId(),
                RelatedType.ARTICLE, articleId, request.getContent(), request.getParentId()));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        commentService.delete(AuthCtx.getUserId(), commentId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/moment/{momentId}")
    public ApiResponse<List<CommentVO>> momentComments(@PathVariable Long momentId) {
        return ApiResponse.success(commentService.list(RelatedType.MOMENT, momentId));
    }

    @GetMapping("/public/article/{articleId}")
    public ApiResponse<List<CommentVO>> articleComments(@PathVariable Long articleId) {
        return ApiResponse.success(commentService.list(RelatedType.ARTICLE, articleId));
    }
}
