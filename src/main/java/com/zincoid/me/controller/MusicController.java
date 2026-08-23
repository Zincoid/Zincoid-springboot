package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.MusicService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    // ──── Private endpoints ───────────────

    @PostMapping
    public ApiResponse<FileVO> upload(@RequestParam("file") MultipartFile file) {
        AuthCtx.requireAdmin();
        return ApiResponse.success(musicService.upload(AuthCtx.getUserId(), file));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long fileId) {
        AuthCtx.requireAdmin();
        musicService.delete(fileId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<PageVO<FileVO>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        AuthCtx.requireLogin();
        return ApiResponse.success(musicService.list(page, size));
    }
}
