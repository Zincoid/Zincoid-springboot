package com.zincoid.me.service;

import java.util.Map;

public interface StorageService {

    Map<String, Long> storageSpace();

    Map<String, Long> userStorage(Long userId);

    void expandCapacity(Long userId, Long capacity);

    void updateCapacity(String username, Long capacity);
}
