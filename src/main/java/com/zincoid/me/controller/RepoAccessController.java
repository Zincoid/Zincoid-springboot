package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
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
    public ApiResp<Void> requestViewer(@PathVariable Long repoId) {
        repoAccessService.view(AuthCtx.getUserId(), repoId);
        return ApiResp.success();
    }

    @PostMapping("/{repoId}/contributors")
    public ApiResp<Void> requestContributor(@PathVariable Long repoId) {
        repoAccessService.contribute(AuthCtx.getUserId(), repoId);
        return ApiResp.success();
    }

    @DeleteMapping("/{repoId}/contributors")
    public ApiResp<Void> leaveContributor(@PathVariable Long repoId) {
        repoAccessService.leave(AuthCtx.getUserId(), repoId);
        return ApiResp.success();
    }

    @PutMapping("/{accessId}/approve")
    public ApiResp<Void> approveAccess(@PathVariable Long accessId) {
        repoAccessService.approve(AuthCtx.getUserId(), accessId);
        return ApiResp.success();
    }

    @PutMapping("/{accessId}/reject")
    public ApiResp<Void> rejectAccess(@PathVariable Long accessId) {
        repoAccessService.reject(AuthCtx.getUserId(), accessId);
        return ApiResp.success();
    }

    @DeleteMapping("/{accessId}")
    public ApiResp<Void> removeAccess(@PathVariable Long accessId) {
        repoAccessService.remove(AuthCtx.getUserId(), accessId);
        return ApiResp.success();
    }

    @GetMapping("/sent/pending")
    public ApiResp<PageVO<RepoAccessVO>> sentPending(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) AccessRole role) {
        return ApiResp.success(repoAccessService.sentPending(AuthCtx.getUserId(), role, page, size));
    }

    @GetMapping("/sent/resolved")
    public ApiResp<PageVO<RepoAccessVO>> sentResolved(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(required = false) AccessRole role) {
        return ApiResp.success(repoAccessService.sentResolved(AuthCtx.getUserId(), role, page, size));
    }

    @GetMapping("/received/pending")
    public ApiResp<PageVO<RepoAccessVO>> receivedPending(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long repoId,
                                                         @RequestParam(required = false) AccessRole role) {
        return ApiResp.success(repoAccessService.receivedPending(AuthCtx.getUserId(), repoId, role, page, size));
    }

    @GetMapping("/received/resolved")
    public ApiResp<PageVO<RepoAccessVO>> receivedResolved(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) Long repoId,
                                                          @RequestParam(required = false) AccessRole role) {
        return ApiResp.success(repoAccessService.receivedResolved(AuthCtx.getUserId(), repoId, role, page, size));
    }
}
