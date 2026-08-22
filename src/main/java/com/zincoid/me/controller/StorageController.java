package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.service.StorageService;
import com.zincoid.me.service.impl.CleanupServiceImpl;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final CleanupServiceImpl cleanupService;

    // ──── Private endpoints ───────────────

    @GetMapping
    public ApiResponse<Map<String, Long>> storageSpace() {
        AuthCtx.requireAdmin();
        return ApiResponse.success(storageService.storageSpace());
    }

    @GetMapping("/user")
    public ApiResponse<Map<String, Long>> userStorage() {
        return ApiResponse.success(storageService.userStorage(AuthCtx.getUserId()));
    }

    @PutMapping("/{userId}/capacity")
    public ApiResponse<Void> updateCapacity(@PathVariable Long userId,
                                            @RequestParam Long capacity) {
        AuthCtx.requireAdmin();
        storageService.updateCapacity(userId, capacity);
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
}
