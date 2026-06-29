package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.NotificationVO;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ──── Private endpoints ────────────────

    @GetMapping("/count")
    public ApiResponse<Long> countUnreadNotifications() {
        return ApiResponse.success(notificationService.countUnread(AuthCtx.getUserId()));
    }

    @GetMapping
    public ApiResponse<List<NotificationVO>> listNotifications() {
        return ApiResponse.success(notificationService.list(AuthCtx.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteOne(id, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAllNotifications() {
        notificationService.deleteAll(AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> readOneNotification(@PathVariable Long id) {
        notificationService.readOne(id, AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @PutMapping("/read")
    public ApiResponse<Void> readAllNotifications() {
        notificationService.readAll(AuthCtx.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/broadcast")
    public ApiResponse<Void> broadcast(@RequestBody Map<String, String> body) {
        AuthCtx.requireAdmin();
        String content = body.get("content");
        if (content == null || content.isBlank())
            return ApiResponse.error(400, "Content is required");
        notificationService.broadcast(AuthCtx.getUserId(), content.trim());
        return ApiResponse.success();
    }
}
