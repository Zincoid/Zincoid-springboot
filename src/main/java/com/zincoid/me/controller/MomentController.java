package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.dto.MomentCreateRequest;
import com.zincoid.me.model.dto.MomentUpdateRequest;
import com.zincoid.me.model.enums.Role;
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

    // ──── Private endpoints ───────────────

    @PostMapping
    public ApiResp<MomentCardVO> createMoment(@Valid @RequestBody MomentCreateRequest request) {
        return ApiResp.success(momentService.create(AuthCtx.getUserId(), request));
    }

    @PutMapping("/{momentId}")
    public ApiResp<MomentCardVO> updateMoment(@PathVariable Long momentId,
                                              @Valid @RequestBody MomentUpdateRequest request) {
        return ApiResp.success(momentService.update(AuthCtx.getUserId(), momentId, request));
    }

    @DeleteMapping("/{momentId}")
    public ApiResp<Void> deleteMoment(@PathVariable Long momentId) {
        momentService.delete(AuthCtx.getUserId(), momentId, AuthCtx.getRole() == Role.ADMIN);
        return ApiResp.success();
    }

    @PutMapping("/{momentId}/pin")
    public ApiResp<Void> pinMoment(@PathVariable Long momentId) {
        AuthCtx.requireAdmin();
        momentService.pin(momentId);
        return ApiResp.success();
    }

    @PutMapping("/{momentId}/unpin")
    public ApiResp<Void> unpinMoment(@PathVariable Long momentId) {
        AuthCtx.requireAdmin();
        momentService.unpin(momentId);
        return ApiResp.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public/random")
    public ApiResp<MomentCardVO> randomMoment() {
        return ApiResp.success(momentService.random());
    }

    @GetMapping("/public/home")
    public ApiResp<List<MomentCardVO>> homeMoments(@RequestParam(defaultValue = "10") int size) {
        return ApiResp.success(momentService.home(size));
    }

    @GetMapping("/public")
    public ApiResp<PageVO<MomentCardVO>> listMoments(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResp.success(momentService.list(page, size, pinned));
    }

    @GetMapping("/public/user/{userId}")
    public ApiResp<PageVO<MomentCardVO>> userMoments(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(defaultValue = "false") boolean pinned) {
        return ApiResp.success(momentService.list(userId, page, size, pinned));
    }

    @GetMapping("/public/{momentId}")
    public ApiResp<MomentDetailVO> momentDetail(@PathVariable Long momentId) {
        return ApiResp.success(momentService.get(momentId));
    }

    @PostMapping("/public/{momentId}/view")
    public ApiResp<Void> momentView(@PathVariable Long momentId) {
        momentService.view(momentId);
        return ApiResp.success();
    }
}
