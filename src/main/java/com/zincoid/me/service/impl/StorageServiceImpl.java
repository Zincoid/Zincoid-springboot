package com.zincoid.me.service.impl;

import com.zincoid.me.service.StorageService;
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
}
