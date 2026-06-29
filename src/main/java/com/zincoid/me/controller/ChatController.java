package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;

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

    @GetMapping("/public")
    public ApiResponse<PageVO<MessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(messageService.list(page, size));
    }
}
