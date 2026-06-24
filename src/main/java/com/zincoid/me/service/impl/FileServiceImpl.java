package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.mapper.UploadFileMapper;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.enums.FileType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.service.FileService;
import com.zincoid.me.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<UploadFileMapper, File> implements FileService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Override
    @Transactional
    public FileVO upload(Long userId, MultipartFile file, RelatedType relatedType, Long relatedId) {
        String filePath = FileUtil.save(file, uploadPath);
        String ext = FileUtil.getExt(file.getOriginalFilename());
        FileType fileType = FileUtil.getType(ext);
        File uploadFile = File.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileType(fileType)
                .fileSize(file.getSize())
                .relatedType(relatedType)
                .relatedId(relatedId)
                .build();
        save(uploadFile);
        return FileVO.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .url("/uploads/" + filePath)
                .fileSize(file.getSize())
                .build();
    }

    @Override
    @Transactional
    public void link(List<String> filePathsOrUrls, RelatedType relatedType, Long relatedId) {
        if (filePathsOrUrls == null || filePathsOrUrls.isEmpty()) return;
        List<String> cleanPaths = filePathsOrUrls.stream()
                .map(p -> p.startsWith("/uploads/") ? p.substring("/uploads/".length()) : p)
                .toList();
        lambdaUpdate()
                .isNull(File::getRelatedType)
                .isNull(File::getRelatedId)
                .in(File::getFilePath, cleanPaths)
                .set(File::getRelatedType, relatedType)
                .set(File::getRelatedId, relatedId)
                .update();
    }

    @Override
    @Transactional
    public void delete(String filePath) {
        if (filePath == null) return;
        String fileName = filePath.startsWith("/uploads/")
                ? filePath.substring("/uploads/".length())
                : filePath;
        FileUtil.delete(fileName, uploadPath);
        remove(new LambdaQueryWrapper<File>().eq(File::getFilePath, fileName));
    }

    @Override
    @Transactional
    public void delete(RelatedType relatedType, Long relatedId) {
        List<File> files = lambdaQuery()
                .eq(File::getRelatedType, relatedType)
                .eq(File::getRelatedId, relatedId)
                .list();
        for (File file : files) {
            FileUtil.delete(file.getFilePath(), uploadPath);
            removeById(file.getId());
        }
        if (!files.isEmpty())
            log.info("Cleaned up {} files for type {}: {}", files.size(), relatedType, relatedId);
    }

    @Override
    @Transactional
    public void cleanup() {
        List<File> allFiles = list();
        Set<String> dbPaths = allFiles.stream().map(File::getFilePath).collect(Collectors.toSet());
        Set<String> diskFiles = FileUtil.list(uploadPath);
        for (File file : allFiles) {
            if (!diskFiles.contains(file.getFilePath())) {
                removeById(file.getId());
                log.info("Removed orphan DB record: {}", file.getFilePath());
            }
        }
        for (String diskFile : diskFiles) {
            if (!dbPaths.contains(diskFile)) {
                FileUtil.delete(diskFile, uploadPath);
                log.info("Removed orphan disk file: {}", diskFile);
            }
        }
    }
}
