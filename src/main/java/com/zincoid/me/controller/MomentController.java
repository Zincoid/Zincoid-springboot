package com.zincoid.me.controller;

import com.zincoid.me.model.dto.MomentCreateRequest;
import com.zincoid.me.model.dto.MomentUpdateRequest;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.MomentDetailVO;
import com.zincoid.me.model.vo.MomentCardVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.utils.AuthCtx;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    // ──── Private endpoints ────────────────

    @PostMapping
    public ApiResponse<MomentCardVO> create(@Valid @RequestBody MomentCreateRequest request) {
        return ApiResponse.success(momentService.create(AuthCtx.getUserId(), request));
    }

    @PutMapping("/{momentId}")
    public ApiResponse<MomentCardVO> update(@PathVariable Long momentId,
                                            @RequestBody MomentUpdateRequest request) {
        return ApiResponse.success(momentService.update(AuthCtx.getUserId(), momentId, request));
    }

    @DeleteMapping("/{momentId}")
    public ApiResponse<Void> delete(@PathVariable Long momentId) {
        momentService.delete(AuthCtx.getUserId(), momentId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResponse.success();
    }

    @PutMapping("/{momentId}/pin")
    public ApiResponse<Void> pin(@PathVariable Long momentId) {
        AuthCtx.requireAdmin();
        momentService.pin(momentId);
        return ApiResponse.success();
    }

    @PutMapping("/{momentId}/unpin")
    public ApiResponse<Void> unpin(@PathVariable Long momentId) {
        AuthCtx.requireAdmin();
        momentService.unpin(momentId);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/random")
    public ApiResponse<MomentCardVO> randomMoment() {
        return ApiResponse.success(momentService.random());
    }

    @GetMapping("/public")
    public ApiResponse<PageVO<MomentCardVO>> listMoments(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResponse.success(momentService.list(page, size, pinned));
    }

    @GetMapping("/public/home")
    public ApiResponse<List<MomentCardVO>> homeFeed(@RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(momentService.home(size));
    }

    @GetMapping("/public/user/{userId}")
    public ApiResponse<PageVO<MomentCardVO>> userMoments(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResponse.success(momentService.list(userId, page, size, pinned));
    }

    @GetMapping("/public/{momentId}")
    public ApiResponse<MomentDetailVO> momentDetail(@PathVariable Long momentId) {
        return ApiResponse.success(momentService.get(momentId));
    }
}
