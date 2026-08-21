package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.service.impl.CleanupServiceImpl;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final CleanupServiceImpl cleanupService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    // ──── Public endpoints ────────────────

    @GetMapping("/storage")
    public ApiResponse<Map<String, Long>> storageSpace() {
        AuthCtx.requireAdmin();
        Map<String, Long> space = new LinkedHashMap<>();
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) Files.createDirectories(path);
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long free = store.getUsableSpace();
            space.put("total", total);
            space.put("free", free);
            space.put("used", total - free);
        } catch (IOException e) {
            log.warn("Failed to get storage space of: {}", uploadPath, e);
            space.put("total", 0L);
            space.put("free", 0L);
            space.put("used", 0L);
        }
        return ApiResponse.success(space);
    }

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
    public ApiResponse<Map<String, Integer>> cleanupFiles(@RequestParam(defaultValue = "false") boolean isLogic) {
        AuthCtx.requireAdmin();
        return ApiResponse.success(cleanupService.cleanupFiles(isLogic));
    }
}
