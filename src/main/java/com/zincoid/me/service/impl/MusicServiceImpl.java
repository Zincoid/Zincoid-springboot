package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.MusicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {

    private final FileService fileService;

    @Override
    public FileVO upload(Long userId, MultipartFile file) {
        return fileService.upload(userId, file, RelatedType.MUSIC, userId);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        File file = fileService.getById(fileId);
        if (file == null) throw new BusinessException(404, "File not found");
        if (file.getRelatedType() != RelatedType.MUSIC)
            throw new BusinessException(400, "Not a music file");
        fileService.delete(fileId);
        log.info("Music deleted: id={}", fileId);
    }

    @Override
    public PageVO<FileVO> list(int page, int size) {
        Page<File> result = fileService.lambdaQuery()
                .eq(File::getRelatedType, RelatedType.MUSIC)
                .orderByDesc(File::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(result, f -> FileVO.builder()
                .id(f.getId())
                .fileName(f.getFileName())
                .filePath(f.getFilePath())
                .url("/uploads/" + f.getFilePath())
                .fileSize(f.getFileSize())
                .build());
    }
}
