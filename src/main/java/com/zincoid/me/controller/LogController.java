package com.zincoid.me.controller;

import ch.qos.logback.classic.Level;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.LogStreamService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogStreamService logStreamService;
    private final FileService fileService;

    // ──── Private endpoints ───────────────

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(defaultValue = "INFO") String level, HttpServletResponse response) {
        AuthCtx.requireAdmin();
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        return logStreamService.subscribe(Level.toLevel(level.toUpperCase(), Level.INFO));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download() {
        AuthCtx.requireAdmin();
        Path logFile = fileService.logFile();
        if (logFile == null) return ResponseEntity.notFound().build();
        Resource resource = new FileSystemResource(logFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + logFile.getFileName() + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(logFile.toFile().length())
                .body(resource);
    }
}
