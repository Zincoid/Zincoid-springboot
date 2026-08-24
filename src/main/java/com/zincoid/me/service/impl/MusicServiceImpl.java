package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Role;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.MusicService;
import com.zincoid.me.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {

    private final FileService fileService;
    private final UserService userService;

    @Override
    public FileVO upload(Long userId, MultipartFile file, boolean isPublic, boolean isAdmin) {
        if (isPublic && !isAdmin)
            throw new BusinessException(403, "No permission to upload public music");
        return fileService.upload(userId, file, RelatedType.MUSIC, isPublic ? 1L : 0L);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long fileId) {
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException(404, "User not found");
        File file = fileService.getById(fileId);
        if (file == null) throw new BusinessException(404, "File not found");
        if (file.getRelatedType() != RelatedType.MUSIC)
            throw new BusinessException(400, "Not a music file");
        if (!file.getUserId().equals(userId))
            throw new BusinessException(403, "No permission to delete this music");
        fileService.delete(fileId);
        log.info("Music deleted: id={}", fileId);
    }

    @Override
    public PageVO<FileVO> list(int page, int size) {
        Page<File> result = fileService.lambdaQuery()
                .eq(File::getRelatedType, RelatedType.MUSIC)
                .eq(File::getRelatedId, 1L)  // Public
                .orderByDesc(File::getCreatedAt)
                .page(Page.of(page, size));
        return toMusicVO(result);
    }

    @Override
    public PageVO<FileVO> list(Long userId, int page, int size, boolean isPublic) {
        Page<File> result = fileService.lambdaQuery()
                .eq(File::getRelatedType, RelatedType.MUSIC)
                .eq(File::getUserId, userId)
                .eq(File::getRelatedId, isPublic ? 1L : 0L)
                .orderByDesc(File::getCreatedAt)
                .page(Page.of(page, size));
        return toMusicVO(result);
    }

    // ──────── Private tool ────────────────────────────────

    private static PageVO<FileVO> toMusicVO(Page<File> result) {
        return PageVO.of(result, f -> FileVO.builder()
                .id(f.getId())
                .fileName(f.getFileName())
                .filePath(f.getFilePath())
                .url("/uploads/" + f.getFilePath())
                .fileSize(f.getFileSize())
                .build());
    }
}
