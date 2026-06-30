package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.mapper.FileMapper;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.enums.FileType;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private final MomentService momentService;
    private final ArticleService articleService;
    private final UserService userService;
    private final MessageService messageService;

    public FileServiceImpl(@Lazy MomentService momentService,
                           @Lazy ArticleService articleService,
                           @Lazy UserService userService,
                           @Lazy MessageService messageService) {
        this.momentService = momentService;
        this.articleService = articleService;
        this.userService = userService;
        this.messageService = messageService;
    }

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
        log.info("File uploaded: path={}, type={}, relation={}:{}", filePath, fileType, relatedType, relatedId);
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
        List<String> paths = filePathsOrUrls.stream()
                .map(p -> p.startsWith("/uploads/") ? p.substring("/uploads/".length()) : p)
                .toList();
        lambdaUpdate()
                .isNull(File::getRelatedType)
                .isNull(File::getRelatedId)
                .in(File::getFilePath, paths)
                .set(File::getRelatedType, relatedType)
                .set(File::getRelatedId, relatedId)
                .update();
        log.info("Files linked: {} -> {}:{}", paths, relatedType, relatedId);
    }

    @Override
    @Transactional
    public void delete(String filePathOrUrl) {
        if (filePathOrUrl == null) return;
        String path = filePathOrUrl.startsWith("/uploads/")
                ? filePathOrUrl.substring("/uploads/".length())
                : filePathOrUrl;
        FileUtil.delete(path, uploadPath);
        lambdaUpdate().eq(File::getFilePath, path).remove();
        log.info("File deleted: path={}", path);
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
            log.info("File deleted: count={}, relation={}:{}", files.size(), relatedType, relatedId);
    }

    @Override
    @Transactional
    public Map<String, Integer> cleanup(boolean isLogic) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int orphanDb = 0, orphanDisk = 0, unlinked = 0, invalidRef = 0;
        List<File> allFiles = list();
        Set<String> dbPaths = allFiles.stream().map(File::getFilePath).collect(Collectors.toSet());
        Set<String> diskFiles = FileUtil.list(uploadPath);
        for (File file : allFiles) {
            if (!diskFiles.contains(file.getFilePath())) {
                removeById(file.getId());
                orphanDb++;
                log.info("Clean up: orphan DB record - {}", file.getId());
            }
        }
        for (String diskFile : diskFiles) {
            if (!dbPaths.contains(diskFile)) {
                FileUtil.delete(diskFile, uploadPath);
                orphanDisk++;
                log.info("Clean up: orphan disk file - {}", diskFile);
            }
        }
        List<File> unlinkedFiles = lambdaQuery()
                .and(w -> w.isNull(File::getRelatedType).or().isNull(File::getRelatedId))
                .list();
        for (File file : unlinkedFiles) {
            FileUtil.delete(file.getFilePath(), uploadPath);
            removeById(file.getId());
            unlinked++;
            log.info("Clean up: unlinked file and record - {}:{}", file.getFilePath(), file.getId());
        }
        if (isLogic) {
            List<File> linked = lambdaQuery()
                    .isNotNull(File::getRelatedType)
                    .isNotNull(File::getRelatedId)
                    .list();
            for (File file : linked) {
                if (!businessExists(file)) {
                    FileUtil.delete(file.getFilePath(), uploadPath);
                    removeById(file.getId());
                    invalidRef++;
                    log.info("Clean up: invalid relation of {}:{} - {}:{}",
                            file.getRelatedType(), file.getRelatedId(), file.getFilePath(), file.getId());
                }
            }
        }
        result.put("orphanDb", orphanDb);
        result.put("orphanDisk", orphanDisk);
        result.put("unlinked", unlinked);
        if (isLogic) result.put("invalidRef", invalidRef);
        log.info("Clean up done: {}", result);
        return result;
    }

    // ──────── Private tool ────────────────────────────────

    private boolean businessExists(File file) {
        Long id = file.getRelatedId();
        return switch (file.getRelatedType()) {
            case MOMENT -> momentService.getById(id) != null;
            case ARTICLE -> articleService.getById(id) != null;
            case AVATAR -> userService.getById(id) != null;
            case CHAT -> messageService.getById(id) != null;
        };
    }
}
