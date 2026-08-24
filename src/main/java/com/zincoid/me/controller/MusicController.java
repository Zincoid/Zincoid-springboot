package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResponse;
import com.zincoid.me.model.enums.Role;
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
    public ApiResponse<FileVO> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "false") boolean isPublic) {
        return ApiResponse.success(musicService.upload(AuthCtx.getUserId(), file, isPublic, AuthCtx.getRole() == Role.ADMIN));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long fileId) {
        musicService.delete(AuthCtx.getUserId(), fileId);
        return ApiResponse.success();
    }

    @GetMapping()
    public ApiResponse<PageVO<FileVO>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(musicService.list(page, size));
    }

    @GetMapping("/user")
    public ApiResponse<PageVO<FileVO>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "false") boolean isPublic) {
        return ApiResponse.success(musicService.list(AuthCtx.getUserId(), page, size, isPublic));
    }
}
