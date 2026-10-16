package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.RequestType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RequestVO;
import com.zincoid.me.service.RequestService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    // ──── Private endpoints ───────────────

    @PostMapping("/{receiverId}")
    public ApiResp<RequestVO> create(@PathVariable Long receiverId,
                                     @RequestParam RequestType type,
                                     @RequestParam(required = false) String meta) {
        return ApiResp.success(requestService.create(AuthCtx.getUserId(), receiverId, type, meta));
    }

    @GetMapping("/sent")
    public ApiResp<PageVO<RequestVO>> sent(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResp.success(requestService.sent(AuthCtx.getUserId(), page, size));
    }

    @GetMapping("/received")
    public ApiResp<PageVO<RequestVO>> received(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResp.success(requestService.received(AuthCtx.getUserId(), page, size, AuthCtx.getRole() == Role.ADMIN));
    }

    @PutMapping("/{requestId}")
    public ApiResp<RequestVO> handle(@PathVariable Long requestId,
                                     @RequestParam Access access) {
        return ApiResp.success(requestService.handle(AuthCtx.getUserId(), requestId, access, AuthCtx.getRole() == Role.ADMIN));
    }

    @DeleteMapping("/{requestId}")
    public ApiResp<Void> delete(@PathVariable Long requestId) {
        requestService.delete(AuthCtx.getUserId(), requestId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResp.success();
    }
}
