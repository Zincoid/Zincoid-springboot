package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping
    public ApiResponse<Map<String, Object>> stats(@RequestParam(defaultValue = "30") int days,
                                                  @RequestParam(defaultValue = "50") int top) {
        if (days <= 0 || days > 365)
            return ApiResponse.badRequest("days must be between 1 and 365");
        if (top < 0 || top > 500)
            return ApiResponse.badRequest("top must be between 0 and 500");
        return ApiResponse.success(statService.stats(days, top));
    }
}
