package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.dto.EmailBroadcastRequest;
import com.zincoid.me.model.vo.NotificationVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.EmailService;
import com.zincoid.me.service.NotificationService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;

    // ──── Private endpoints ───────────────

    @GetMapping("/count")
    public ApiResp<Long> countUnreadNotifications() {
        return ApiResp.success(notificationService.countUnread(AuthCtx.getUserId()));
    }

    @GetMapping
    public ApiResp<PageVO<NotificationVO>> listNotifications(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "5") int size) {
        return ApiResp.success(notificationService.list(AuthCtx.getUserId(), page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResp<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteOne(id, AuthCtx.getUserId());
        return ApiResp.success();
    }

    @DeleteMapping
    public ApiResp<Void> deleteAllNotifications() {
        notificationService.deleteAll(AuthCtx.getUserId());
        return ApiResp.success();
    }

    @PutMapping("/{id}/read")
    public ApiResp<Void> readOneNotification(@PathVariable Long id) {
        notificationService.readOne(id, AuthCtx.getUserId());
        return ApiResp.success();
    }

    @PutMapping("/read")
    public ApiResp<Void> readAllNotifications() {
        notificationService.readAll(AuthCtx.getUserId());
        return ApiResp.success();
    }

    @PostMapping("/broadcast")
    public ApiResp<Void> broadcast(@RequestBody Map<String, String> body) {
        AuthCtx.requireAdmin();
        notificationService.broadcast(AuthCtx.getUserId(), body.get("content"));
        return ApiResp.success();
    }

    @PostMapping("/broadcast/email")
    public ApiResp<Void> broadcast(@Valid @RequestBody EmailBroadcastRequest request) {
        AuthCtx.requireAdmin();
        emailService.sendBroadcast(request.getSubject().trim(), request.getContent().trim(),
                Boolean.TRUE.equals(request.getForce()));
        return ApiResp.success();
    }
}
