package com.zincoid.me.service;

public interface AiService {

    boolean isAi(Long userId);

    void chat(Long userId);

    void comment(Long userId, Long commentId);
}
