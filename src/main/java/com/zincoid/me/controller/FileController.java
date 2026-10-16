package com.zincoid.me.controller;

import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.service.FileService;
import com.zincoid.me.utils.AuthCtx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ──── Private endpoints ───────────────

    @PostMapping("/upload")
    public ApiResp<FileVO> uploadFile(@RequestParam("file") MultipartFile file) {
        return ApiResp.success(fileService.upload(AuthCtx.getUserId(), file));
    }
}
