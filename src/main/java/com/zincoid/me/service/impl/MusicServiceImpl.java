package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.Error;
import com.zincoid.me.model.enums.BizType;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {

    private final FileService fileService;
    private final UserService userService;

    @Override
    public FileVO upload(Long userId, MultipartFile file, boolean isPublic, boolean isAdmin) {
        if (isPublic && !isAdmin)
            throw new BusinessException(Error.NO_PERMISSION_UPLOAD_MUSIC);
        FileVO vo = fileService.upload(userId, file);
        fileService._link(List.of(vo.getId()), BizType.MUSIC, isPublic ? 1L : 0L, userId);
        return vo;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long fileId) {
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException(Error.USER_NOT_FOUND);
        File file = fileService.getById(fileId);
        if (file == null) throw new BusinessException(Error.FILE_NOT_FOUND);
        if (file.getRelatedType() != BizType.MUSIC)
            throw new BusinessException(Error.NOT_A_MUSIC_FILE);
        if (!file.getUserId().equals(userId))
            throw new BusinessException(Error.NO_PERMISSION_DELETE_MUSIC);
        fileService.delete(fileId);
        log.info("Music deleted: id={}", fileId);
    }

    @Override
    public PageVO<FileVO> list(int page, int size) {
        Page<File> result = fileService.lambdaQuery()
                .eq(File::getRelatedType, BizType.MUSIC)
                .eq(File::getRelatedId, 1L)  // Public
                .orderByDesc(File::getCreatedAt)
                .page(Page.of(page, size));
        return toMusicVO(result);
    }

    @Override
    public PageVO<FileVO> list(Long userId, int page, int size) {
        Page<File> result = fileService.lambdaQuery()
                .eq(File::getRelatedType, BizType.MUSIC)
                .eq(File::getUserId, userId)
                .eq(File::getRelatedId, 0L)  // Private
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
