package com.zincoid.me.controller;

import com.zincoid.me.model.dto.CommentCreateRequest;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.ApiResponse;
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
    public ApiResponse<PageVO<CommentVO>> momentComments(@PathVariable Long momentId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(commentService.list(RelatedType.MOMENT, momentId, page, size));
    }

    @GetMapping("/public/article/{articleId}")
    public ApiResponse<PageVO<CommentVO>> articleComments(@PathVariable Long articleId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(commentService.list(RelatedType.ARTICLE, articleId, page, size));
    }

    @GetMapping("/public/replies/{parentId}")
    public ApiResponse<List<CommentVO>> replies(@PathVariable Long parentId) {
        return ApiResponse.success(commentService.replies(parentId));
    }
}
