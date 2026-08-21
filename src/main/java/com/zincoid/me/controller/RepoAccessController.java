package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.po.RepoAccess;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoAccessController {

    private final RepoAccessService repoAccessService;

    // ──── Private endpoints ───────────────

    @PostMapping("/{repoId}/access")
    public ApiResponse<Void> requestAccess(@PathVariable Long repoId) {
        repoAccessService.request(AuthCtx.getUserId(), repoId);
        return ApiResponse.success();
    }

    @PutMapping("/access/{accessId}/approve")
    public ApiResponse<Void> approveAccess(@PathVariable Long accessId) {
        repoAccessService.approve(AuthCtx.getUserId(), accessId);
        return ApiResponse.success();
    }

    @PutMapping("/access/{accessId}/reject")
    public ApiResponse<Void> rejectAccess(@PathVariable Long accessId) {
        repoAccessService.reject(AuthCtx.getUserId(), accessId);
        return ApiResponse.success();
    }

    @DeleteMapping("/access/{accessId}")
    public ApiResponse<Void> removeAccess(@PathVariable Long accessId) {
        repoAccessService.remove(AuthCtx.getUserId(), accessId);
        return ApiResponse.success();
    }

    @GetMapping("/access/sent/pending")
    public ApiResponse<PageVO<RepoAccess>> sentPending(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(repoAccessService.sentPending(AuthCtx.getUserId(), page, size));
    }

    @GetMapping("/access/sent/resolved")
    public ApiResponse<PageVO<RepoAccess>> sentResolved(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(repoAccessService.sentResolved(AuthCtx.getUserId(), page, size));
    }

    @GetMapping("/access/received/pending")
    public ApiResponse<PageVO<RepoAccess>> receivedPending(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(repoAccessService.receivedPending(AuthCtx.getUserId(), page, size));
    }

    @GetMapping("/access/received/resolved")
    public ApiResponse<PageVO<RepoAccess>> receivedResolved(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(repoAccessService.receivedResolved(AuthCtx.getUserId(), page, size));
    }
}
