package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.FileMapper;
import com.zincoid.me.model.enums.*;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.MessageService;
import com.zincoid.me.service.MomentService;
import com.zincoid.me.service.RepoAccessService;
import com.zincoid.me.service.RepoService;
import com.zincoid.me.service.RequestService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${logging.file.name}")
    private String logFilePath;

    private final UserService userService;
    private final MomentService momentService;
    private final ArticleService articleService;
    private final RepoService repoService;
    private final RepoAccessService repoAccessService;
    private final MessageService messageService;
    private final RequestService requestService;

    public FileServiceImpl(UserService userService,
                           @Lazy MomentService momentService,
                           @Lazy ArticleService articleService,
                           @Lazy RepoService repoService,
                           @Lazy RepoAccessService repoAccessService,
                           @Lazy MessageService messageService,
                           @Lazy RequestService requestService) {
        this.userService = userService;
        this.momentService = momentService;
        this.articleService = articleService;
        this.repoService = repoService;
        this.repoAccessService = repoAccessService;
        this.messageService = messageService;
        this.requestService = requestService;
    }

    @Override
    public File get(String path) {
        if (path == null || path.isBlank()) return null;
        List<File> files = lambdaQuery().eq(File::getFilePath, path).list();
        return files.isEmpty() ? null : files.getFirst();
    }

    @Override
    public boolean accessible(File file, Long userId, Role role) {
        if (file == null) return false;
        if (file.getRelatedType() == null)
            return userId != null && userId.equals(file.getUserId());
        boolean isAdmin = role == Role.ADMIN;
        return switch (file.getRelatedType()) {
            case AVATAR, CHAT -> true;
            case MUSIC -> {
                if (file.getRelatedId() != null && file.getRelatedId() == 1L) yield true;
                boolean isOwner = userId != null && userId.equals(file.getUserId());
                if (isOwner || isAdmin) yield true;
                yield userId != null && requestService.isMusicShared(userId, file);
            }
            case MOMENT -> {
                Moment moment = momentService.getById(file.getRelatedId());
                if (moment == null || moment.getStatus() == Status.DISABLED) yield false;
                boolean isOwner = userId != null && userId.equals(moment.getUserId());
                yield moment.getVisibility() != Visibility.PRIVATE || isOwner || isAdmin;
            }
            case ARTICLE -> {
                Article article = articleService.getById(file.getRelatedId());
                if (article == null || article.getStatus() == Status.DISABLED) yield false;
                boolean isOwner = userId != null && userId.equals(article.getUserId());
                yield article.getVisibility() != Visibility.PRIVATE || isOwner || isAdmin;
            }
            case REPO -> {
                Repo repo = repoService.getById(file.getRelatedId());
                if (repo == null || repo.getStatus() == Status.DISABLED) yield false;
                boolean isOwner = userId != null && userId.equals(repo.getUserId());
                if (repo.getVisibility() == Visibility.PUBLIC || isOwner || isAdmin) yield true;
                if (repo.getVisibility() == Visibility.RESTRICTED)
                    yield userId != null && repoAccessService.authorize(userId, repo.getId(), AccessRole.VIEWER);
                yield false;
            }
        };
    }

    @Override
    public boolean cacheable(File file) {
        if (file == null || file.getRelatedType() == null) return false;
        return switch (file.getRelatedType()) {
            case AVATAR, CHAT -> true;
            case MUSIC -> file.getRelatedId() != null && file.getRelatedId() == 1L;
            case MOMENT -> {
                Moment moment = momentService.getById(file.getRelatedId());
                yield moment != null && moment.getStatus() != Status.DISABLED
                        && moment.getVisibility() == Visibility.PUBLIC;
            }
            case ARTICLE -> {
                Article article = articleService.getById(file.getRelatedId());
                yield article != null && article.getStatus() != Status.DISABLED
                        && article.getVisibility() == Visibility.PUBLIC;
            }
            case REPO -> {
                Repo repo = repoService.getById(file.getRelatedId());
                if (repo == null || repo.getStatus() == Status.DISABLED) yield false;
                yield repo.getVisibility() == Visibility.PUBLIC;
            }
        };
    }

    @Override
    @Transactional
    public FileVO upload(Long userId, MultipartFile file) {
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException(404, "User not found");
        long available = Math.max(user.getCapacity() - totalSize(userId), 0L);
        if (file.getSize() > available)
            throw new BusinessException(400, "Insufficient personal storage space");
        if (file.getSize() > FileUtil.usableSpace(uploadPath))
            throw new BusinessException(500, "Insufficient server disk space");
        String filePath = FileUtil.save(file, uploadPath);
        String ext = FileUtil.getExt(file.getOriginalFilename());
        FileType fileType = FileUtil.getType(ext);
        File uploadFile = File.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileType(fileType)
                .fileSize(file.getSize())
                .build();
        save(uploadFile);
        log.info("File uploaded: path={}, type={}", filePath, fileType);
        return FileVO.builder()
                .id(uploadFile.getId())
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .url("/uploads/" + filePath)
                .fileSize(file.getSize())
                .build();
    }

    @Override
    public void _link(List<Long> fileIds, RelatedType relatedType, Long relatedId, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) return;
        List<File> unbound = lambdaQuery()
                .in(File::getId, fileIds)
                .isNull(File::getRelatedType)
                .isNull(File::getRelatedId)
                .list();
        claimUnbound(unbound, relatedType, relatedId, userId);
    }

    @Override
    @Transactional
    public void link(List<String> filePathsOrUrls, RelatedType relatedType, Long relatedId, Long userId) {
        if (filePathsOrUrls == null || filePathsOrUrls.isEmpty()) return;
        List<String> paths = filePathsOrUrls.stream()
                .map(p -> p.startsWith("/uploads/") ? p.substring("/uploads/".length()) : p)
                .toList();
        List<File> unbound = lambdaQuery()
                .in(File::getFilePath, paths)
                .isNull(File::getRelatedType)
                .isNull(File::getRelatedId)
                .list();
        claimUnbound(unbound, relatedType, relatedId, userId);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        File file = getById(fileId);
        if (file == null) return;
        delete(file.getFilePath());
    }

    @Override
    @Transactional
    public void delete(String filePathOrUrl) {
        if (filePathOrUrl == null) return;
        String path = filePathOrUrl.startsWith("/uploads/")
                ? filePathOrUrl.substring("/uploads/".length())
                : filePathOrUrl;
        lambdaUpdate().eq(File::getFilePath, path).remove();
        if (lambdaQuery().eq(File::getFilePath, path).count() == 0)
            FileUtil.delete(path, uploadPath);
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
            removeById(file.getId());
            if (lambdaQuery().eq(File::getFilePath, file.getFilePath()).count() == 0)
                FileUtil.delete(file.getFilePath(), uploadPath);
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
                log.info("Cleanup: orphan DB record - {}", file.getId());
            }
        }
        for (String diskFile : diskFiles) {
            if (!dbPaths.contains(diskFile)) {
                FileUtil.delete(diskFile, uploadPath);
                orphanDisk++;
                log.info("Cleanup: orphan disk file - {}", diskFile);
            }
        }
        List<File> unlinkedFiles = lambdaQuery()
                .and(w -> w.isNull(File::getRelatedType).or().isNull(File::getRelatedId))
                .list();
        for (File file : unlinkedFiles) {
            FileUtil.delete(file.getFilePath(), uploadPath);
            removeById(file.getId());
            unlinked++;
            log.info("Cleanup: unlinked file and record - {}:{}", file.getFilePath(), file.getId());
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
                    log.info("Cleanup: invalid relation of {}:{} - {}:{}",
                            file.getRelatedType(), file.getRelatedId(), file.getFilePath(), file.getId());
                }
            }
        }
        result.put("orphanDb", orphanDb);
        result.put("orphanDisk", orphanDisk);
        result.put("unlinked", unlinked);
        if (isLogic) result.put("invalidRef", invalidRef);
        log.info("Cleanup done: {}", result);
        return result;
    }

    @Override
    @Transactional
    public int cleanupUnlinked(Long userId) {
        List<File> files = lambdaQuery()
                .eq(File::getUserId, userId)
                .and(w -> w.isNull(File::getRelatedType).or().isNull(File::getRelatedId))
                .list();
        for (File file : files) {
            FileUtil.delete(file.getFilePath(), uploadPath);
            removeById(file.getId());
        }
        if (!files.isEmpty())
            log.info("File unlinked cleared: user={}, count={}", userId, files.size());
        return files.size();
    }

    @Override
    public int cleanupCache() {
        int count = FileUtil.clear(Paths.get(uploadPath, FileUtil.CACHE_FOLDER).toString());
        log.info("Cache cleared: count={}", count);
        return count;
    }

    @Override
    public long totalSize(Long userId) {
        return lambdaQuery()
                .select(File::getFileSize)
                .eq(File::getUserId, userId)
                .list().stream()
                .mapToLong(File::getFileSize).sum();
    }

    @Override
    public Path logFile() {
        Path path = Paths.get(logFilePath);
        return Files.exists(path) && Files.isRegularFile(path) ? path : null;
    }

    // ──────── Private tool ────────────────────────────────

    private void claimUnbound(List<File> unbound, RelatedType relatedType, Long relatedId, Long userId) {
        for (File file : unbound)
            if (userId == null || !userId.equals(file.getUserId()))
                throw new BusinessException(403, "Permission denied");
        if (unbound.isEmpty()) return;
        lambdaUpdate()
                .isNull(File::getRelatedType)
                .isNull(File::getRelatedId)
                .in(File::getId, unbound.stream().map(File::getId).toList())
                .set(File::getRelatedType, relatedType)
                .set(File::getRelatedId, relatedId)
                .update();
        log.info("Files linked: {} -> {}:{}", unbound.stream().map(File::getFileName).toList(), relatedType, relatedId);
    }

    private boolean businessExists(File file) {
        Long id = file.getRelatedId();
        return switch (file.getRelatedType()) {
            case MOMENT -> momentService.getById(id) != null;
            case ARTICLE -> articleService.getById(id) != null;
            case AVATAR -> userService.getById(id) != null;
            case CHAT -> messageService.getById(id) != null;
            case REPO -> repoService.getById(id) != null;
            case MUSIC -> id == 0 || id == 1;
        };
    }
}
