package com.zincoid.me.controller;

import com.zincoid.me.model.po.Config;
import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.service.ConfigService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // ──── Private endpoints ────────────────

    @GetMapping("/all")
    public ApiResponse<List<Config>> listConfigs() {
        AuthCtx.requireAdmin();
        return ApiResponse.success(configService.list());
    }

    @PutMapping("/{key}")
    public ApiResponse<Void> updateConfig(@PathVariable String key,
                                          @RequestParam String value) {
        AuthCtx.requireAdmin();
        configService.update(key, value);
        return ApiResponse.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public")
    public ApiResponse<Map<String, String>> mapConfig() {
        return ApiResponse.success(configService.map());
    }
}
