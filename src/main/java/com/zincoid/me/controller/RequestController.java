package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
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
    public ApiResponse<RequestVO> create(@PathVariable Long receiverId,
                                         @RequestParam RequestType type,
                                         @RequestParam(required = false) String meta) {
        return ApiResponse.success(requestService.create(AuthCtx.getUserId(), receiverId, type, meta));
    }

    @GetMapping("/sent")
    public ApiResponse<PageVO<RequestVO>> sent(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(requestService.sent(AuthCtx.getUserId(), page, size));
    }

    @GetMapping("/received")
    public ApiResponse<PageVO<RequestVO>> received(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(requestService.received(AuthCtx.getUserId(), page, size, AuthCtx.getRole() == Role.ADMIN));
    }

    @PutMapping("/{requestId}")
    public ApiResponse<RequestVO> handle(@PathVariable Long requestId,
                                         @RequestParam Access access) {
        return ApiResponse.success(requestService.handle(AuthCtx.getUserId(), requestId, access, AuthCtx.getRole() == Role.ADMIN));
    }

    @DeleteMapping("/{requestId}")
    public ApiResponse<Void> delete(@PathVariable Long requestId) {
        requestService.delete(AuthCtx.getUserId(), requestId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }
}
