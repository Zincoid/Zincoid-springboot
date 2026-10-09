package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.MessageStreamService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final MessageStreamService messageStreamService;

    // ──── Private endpoints ───────────────

    @PostMapping
    public ApiResponse<MessageVO> send(@RequestParam(required = false) String content,
                                       @RequestParam(required = false) String file) {
        return ApiResponse.success(messageService.send(AuthCtx.getUserId(), content, file));
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> delete(@PathVariable Long messageId) {
        messageService.delete(AuthCtx.getUserId(), messageId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/stream")
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        return messageStreamService.subscribe();
    }

    @GetMapping("/public")
    public ApiResponse<PageVO<MessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(messageService.list(page, size));
    }
}
