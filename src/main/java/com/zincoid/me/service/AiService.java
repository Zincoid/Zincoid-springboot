package com.zincoid.me.service;

public interface AiService {

    boolean isAi(Long userId);

    void chat(Long userId, Long messageId);

    void comment(Long userId, Long commentId);
}
