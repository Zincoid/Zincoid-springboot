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
        return ApiResponse.success(statService.stats(days, top));
    }
}
