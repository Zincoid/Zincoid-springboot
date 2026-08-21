package com.zincoid.me.controller;

import ch.qos.logback.classic.Level;
import com.zincoid.me.service.LogStreamService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogStreamService logStreamService;

    // ──── Private endpoints ───────────────

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(defaultValue = "INFO") String level) {
        AuthCtx.requireAdmin();
        return logStreamService.subscribe(Level.toLevel(level.toUpperCase(), Level.INFO));
    }
}
