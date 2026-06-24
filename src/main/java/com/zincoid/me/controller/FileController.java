package com.zincoid.me.controller;

import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.ApiResponse;
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

    // ──── Private endpoints ────────────────

    @PostMapping("/upload")
    public ApiResponse<FileVO> uploadFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "relatedType", required = false) RelatedType relatedType,
                                          @RequestParam(value = "relatedId", required = false) Long relatedId) {
        return ApiResponse.success(fileService.upload(AuthCtx.getUserId(), file, relatedType, relatedId));
    }

    @DeleteMapping("/cleanup")
    public ApiResponse<Void> cleanupFiles() {
        AuthCtx.requireAdmin();
        fileService.cleanup();
        return ApiResponse.success();
    }
}
