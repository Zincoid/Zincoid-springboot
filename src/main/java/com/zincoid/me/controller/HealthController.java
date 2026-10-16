package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    // ──── Public endpoints ────────────────

    @GetMapping
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }

    @GetMapping("/version")
    public ApiResponse<Map<String, String>> version() {
        BuildProperties build = buildPropertiesProvider.getIfAvailable();
        Map<String, String> info = new LinkedHashMap<>();
        info.put("version", build != null ? build.getVersion() : "dev");
        info.put("build", build != null && build.getTime() != null
                ? Long.toString(build.getTime().toEpochMilli(), 36) : null);
        info.put("time", build != null && build.getTime() != null
                ? build.getTime().toString() : null);
        return ApiResponse.success(info);
    }
}
