package com.zincoid.me.service;

import java.util.Map;

public interface CleanupService {

    Map<String, Integer> cleanupRecords();

    Map<String, Integer> cleanupFiles(boolean isLogic);

    int cleanupUnlinkedFiles(Long userId);

    int cleanupCacheFiles();

    int cleanupExpiredRequests(int retentionDays);
}
