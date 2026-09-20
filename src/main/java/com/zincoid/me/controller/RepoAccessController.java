package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.AccessRole;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RepoAccessVO;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repos/access")
@RequiredArgsConstructor
public class RepoAccessController {

    private final RepoAccessService repoAccessService;

    // ──── Private endpoints ───────────────

    @PostMapping("/{repoId}/viewers")
    public ApiResponse<Void> requestViewer(@PathVariable Long repoId) {
        repoAccessService.view(AuthCtx.getUserId(), repoId);
        return ApiResponse.success();
    }

    @PostMapping("/{repoId}/contributors")
    public ApiResponse<Void> requestContributor(@PathVariable Long repoId) {
        repoAccessService.contribute(AuthCtx.getUserId(), repoId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{repoId}/contributors")
    public ApiResponse<Void> leaveContributor(@PathVariable Long repoId) {
        repoAccessService.leave(AuthCtx.getUserId(), repoId);
        return ApiResponse.success();
    }

    @PutMapping("/{accessId}/approve")
    public ApiResponse<Void> approveAccess(@PathVariable Long accessId) {
        repoAccessService.approve(AuthCtx.getUserId(), accessId);
        return ApiResponse.success();
    }

    @PutMapping("/{accessId}/reject")
    public ApiResponse<Void> rejectAccess(@PathVariable Long accessId) {
        repoAccessService.reject(AuthCtx.getUserId(), accessId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{accessId}")
    public ApiResponse<Void> removeAccess(@PathVariable Long accessId) {
        repoAccessService.remove(AuthCtx.getUserId(), accessId);
        return ApiResponse.success();
    }

    @GetMapping("/sent/pending")
    public ApiResponse<PageVO<RepoAccessVO>> sentPending(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long repoId,
                                                         @RequestParam(required = false) AccessRole role) {
        return ApiResponse.success(repoAccessService.sentPending(AuthCtx.getUserId(), repoId, role, page, size));
    }

    @GetMapping("/sent/resolved")
    public ApiResponse<PageVO<RepoAccessVO>> sentResolved(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) Long repoId,
                                                          @RequestParam(required = false) AccessRole role) {
        return ApiResponse.success(repoAccessService.sentResolved(AuthCtx.getUserId(), repoId, role, page, size));
    }

    @GetMapping("/received/pending")
    public ApiResponse<PageVO<RepoAccessVO>> receivedPending(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             @RequestParam(required = false) Long repoId,
                                                             @RequestParam(required = false) AccessRole role) {
        return ApiResponse.success(repoAccessService.receivedPending(AuthCtx.getUserId(), repoId, role, page, size));
    }

    @GetMapping("/received/resolved")
    public ApiResponse<PageVO<RepoAccessVO>> receivedResolved(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              @RequestParam(required = false) Long repoId,
                                                              @RequestParam(required = false) AccessRole role) {
        return ApiResponse.success(repoAccessService.receivedResolved(AuthCtx.getUserId(), repoId, role, page, size));
    }
}
