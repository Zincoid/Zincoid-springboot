package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
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
    public ApiResponse<Map<String, Long>> storageSpace() {
        AuthCtx.requireAdmin();
        return ApiResponse.success(storageService.storageSpace());
    }

    @DeleteMapping("/cache")
    public ApiResponse<Integer> clearCache() {
        AuthCtx.requireAdmin();
        return ApiResponse.success(cleanupService.cleanupCacheFiles());
    }

    @GetMapping("/user")
    public ApiResponse<Map<String, Long>> userStorage() {
        return ApiResponse.success(storageService.userStorage(AuthCtx.getUserId()));
    }

    @PutMapping("/{username}/capacity")
    public ApiResponse<Void> updateCapacity(@PathVariable String username,
                                            @RequestParam Long capacity) {
        AuthCtx.requireAdmin();
        storageService.updateCapacity(username, capacity);
        return ApiResponse.success();
    }

    @PostMapping("/cleanup")
    public ApiResponse<Map<String, Integer>> cleanupRecords() {
        AuthCtx.requireAdmin();
        return ApiResponse.success(cleanupService.cleanupRecords());
    }

    @DeleteMapping("/cleanup")
    public ApiResponse<Map<String, Integer>> cleanupFiles(@RequestParam(defaultValue = "false") boolean isLogic) {
        AuthCtx.requireAdmin();
        return ApiResponse.success(cleanupService.cleanupFiles(isLogic));
    }

    @DeleteMapping("/unlinked")
    public ApiResponse<Integer> cleanupUnlinkedFiles() {
        return ApiResponse.success(cleanupService.cleanupUnlinkedFiles(AuthCtx.getUserId()));
    }
}
