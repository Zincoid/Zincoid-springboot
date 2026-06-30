package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.service.impl.CleanupServiceImpl;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final CleanupServiceImpl cleanupService;

    // ──── Public endpoints ────────────────

    @GetMapping
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }

    @PostMapping("/cleanup")
    public ApiResponse<Map<String, Integer>> cleanupRecords() {
        AuthCtx.requireAdmin();
        return ApiResponse.success(cleanupService.cleanupRecords());
    }

    @DeleteMapping("/cleanup")
    public ApiResponse<Void> cleanupFiles(@RequestParam(defaultValue = "false") boolean isLogic) {
        AuthCtx.requireAdmin();
        cleanupService.cleanupFiles(isLogic);
        return ApiResponse.success();
    }
}
