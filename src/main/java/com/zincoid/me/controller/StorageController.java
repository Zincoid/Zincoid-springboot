package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.service.CleanupService;
import com.zincoid.me.service.StorageService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final CleanupService cleanupService;

    // ──── Private endpoints ───────────────

    @GetMapping
    public ApiResp<Map<String, Long>> storageSpace() {
        AuthCtx.requireAdmin();
        return ApiResp.success(storageService.storageSpace());
    }

    @DeleteMapping("/cache")
    public ApiResp<Integer> clearCache() {
        AuthCtx.requireAdmin();
        return ApiResp.success(cleanupService.cleanupCacheFiles());
    }

    @GetMapping("/user")
    public ApiResp<Map<String, Long>> userStorage() {
        return ApiResp.success(storageService.userStorage(AuthCtx.getUserId()));
    }

    @PutMapping("/{username}/capacity")
    public ApiResp<Void> updateCapacity(@PathVariable String username,
                                        @RequestParam Long capacity) {
        AuthCtx.requireAdmin();
        storageService.updateCapacity(username, capacity);
        return ApiResp.success();
    }

    @PostMapping("/cleanup")
    public ApiResp<Map<String, Integer>> cleanupRecords() {
        AuthCtx.requireAdmin();
        return ApiResp.success(cleanupService.cleanupRecords());
    }

    @DeleteMapping("/cleanup")
    public ApiResp<Map<String, Integer>> cleanupFiles(@RequestParam(defaultValue = "false") boolean isLogic) {
        AuthCtx.requireAdmin();
        return ApiResp.success(cleanupService.cleanupFiles(isLogic));
    }

    @DeleteMapping("/unlinked")
    public ApiResp<Integer> cleanupUnlinkedFiles() {
        return ApiResp.success(cleanupService.cleanupUnlinkedFiles(AuthCtx.getUserId()));
    }
}
