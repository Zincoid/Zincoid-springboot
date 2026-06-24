package com.zincoid.me.utils;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public final class FileUtil {

    public static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    public static final Set<String> VIDEO_EXTS = Set.of("mp4", "webm", "ogg", "mov", "avi");
    public static final Set<String> AUDIO_EXTS = Set.of("mp3", "wav", "ogg", "aac", "flac");

    private FileUtil() {}

    public static Set<String> list(String path) {
        Path dir = Paths.get(path);
        if (!Files.exists(dir)) return Set.of();
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.warn("Failed to list files in: {}", path, e);
            return Set.of();
        }
    }

    public static String save(MultipartFile file, String path) {
        if (file.isEmpty())
            throw new BusinessException("File is empty");
        String extension = getExt(file.getOriginalFilename());
        if (!isAllowed(extension))
            throw new BusinessException("Unsupported file type: " + extension);
        String filename = UUID.randomUUID() + "." + extension;
        Path targetPath = Paths.get(path, filename);
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
            log.info("File saved: {}", targetPath);
            return filename;
        } catch (IOException e) {
            log.error("File save failed", e);
            throw new BusinessException("File save failed");
        }
    }

    public static void delete(String filename, String path) {
        if (filename == null || filename.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(path, filename));
            log.info("File deleted: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    public static String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static FileType getType(String ext) {
        if (isVideo(ext)) return FileType.VIDEO;
        if (isAudio(ext)) return FileType.AUDIO;
        if (isImage(ext)) return FileType.IMAGE;
        return FileType.OTHER;
    }

    public static boolean isImage(String ext) {
        return ext != null && IMAGE_EXTS.contains(ext);
    }

    public static boolean isVideo(String ext) {
        return ext != null && VIDEO_EXTS.contains(ext);
    }

    public static boolean isAudio(String ext) {
        return ext != null && AUDIO_EXTS.contains(ext);
    }

    private static boolean isAllowed(String ext) {
        return isImage(ext) || isVideo(ext) || isAudio(ext);
    }
}
