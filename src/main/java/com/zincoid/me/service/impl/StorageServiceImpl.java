package com.zincoid.me.service.impl;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.StorageService;
import com.zincoid.me.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final FileService fileService;
    private final UserService userService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Override
    public Map<String, Long> storageSpace() {
        Map<String, Long> space = new LinkedHashMap<>();
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) Files.createDirectories(path);
            FileStore store = Files.getFileStore(path);
            long total = store.getTotalSpace();
            long free = store.getUsableSpace();
            space.put("total", total);
            space.put("free", free);
            space.put("used", total - free);
        } catch (IOException e) {
            log.warn("Failed to get storage space of: {}", uploadPath, e);
            space.put("total", 0L);
            space.put("free", 0L);
            space.put("used", 0L);
        }
        return space;
    }

    @Override
    public Map<String, Long> userStorage(Long userId) {
        User user = userService.getById(userId);
        if (user == null)
            throw new BusinessException(404, "User not found");
        long used = fileService.lambdaQuery()
                .select(File::getFileSize)
                .eq(File::getUserId, userId)
                .list().stream()
                .mapToLong(File::getFileSize).sum();
        long capacity = user.getCapacity();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("capacity", capacity);
        result.put("used", used);
        result.put("available", Math.max(capacity - used, 0L));
        return result;
    }
}
