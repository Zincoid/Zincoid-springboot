package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.po.Config;
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

    // ──── Private endpoints ───────────────

    @GetMapping("/all")
    public ApiResp<List<Config>> listConfigs() {
        AuthCtx.requireAdmin();
        return ApiResp.success(configService.list());
    }

    @PutMapping("/{key}")
    public ApiResp<Void> updateConfig(@PathVariable String key,
                                      @RequestParam String value) {
        AuthCtx.requireAdmin();
        configService.update(key, value);
        return ApiResp.success();
    }

    // ──── Public endpoints ────────────────

    @GetMapping("/public")
    public ApiResp<Map<String, String>> mapConfig() {
        return ApiResp.success(configService.map());
    }
}
