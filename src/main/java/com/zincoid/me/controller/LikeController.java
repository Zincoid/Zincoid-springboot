package com.zincoid.me.controller;

import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // ──── Private endpoints ────────────────

    @PostMapping("/{targetType}/{targetId}")
    public ApiResponse<Map<String, Object>> toggle(@PathVariable RelatedType targetType,
                                                   @PathVariable Long targetId) {
        return ApiResponse.success(Map.of(
                "liked", likeService.toggle(AuthCtx.getUserId(), targetType, targetId),
                "count", likeService.count(targetType, targetId)
        ));
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/{targetType}/{targetId}/status")
    public ApiResponse<Map<String, Object>> status(@PathVariable RelatedType targetType,
                                                   @PathVariable Long targetId) {
        return ApiResponse.success(Map.of(
                "liked", likeService.liked(AuthCtx.getUserId(), targetType, targetId),
                "count", likeService.count(targetType, targetId)
        ));
    }
}
