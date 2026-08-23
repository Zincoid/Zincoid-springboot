package com.zincoid.me.service.impl;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.model.po.User;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.StorageService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        long total = FileUtil.totalSpace(uploadPath);
        long free = FileUtil.usableSpace(uploadPath);
        long cache = FileUtil.dirSize(Paths.get(uploadPath, FileUtil.CACHE_FOLDER).toString());
        long used = FileUtil.dirSize(uploadPath) - cache;
        Map<String, Long> space = new LinkedHashMap<>();
        space.put("total", total);
        space.put("other", Math.max(total - free - used - cache, 0L));
        space.put("used", used);
        space.put("cache", cache);
        space.put("free", free);
        return space;
    }

    @Override
    public Map<String, Long> userStorage(Long userId) {
        User user = userService.getById(userId);
        if (user == null)
            throw new BusinessException(404, "User not found");
        long used = fileService.totalSize(userId);
        long capacity = user.getCapacity();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("capacity", capacity);
        result.put("used", used);
        result.put("available", Math.max(capacity - used, 0L));
        return result;
    }

    @Override
    public void updateCapacity(String username, Long capacity) {
        if (capacity == null || capacity < 0)
            throw new BusinessException(400, "Capacity must be a non-negative number");
        User user = userService.lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) throw new BusinessException(404, "User not found");
        user.setCapacity(capacity);
        userService.updateById(user);
        log.info("User capacity updated: id={}, username={}, capacity={}", user.getId(), user.getUsername(), capacity);
    }
}
