package com.zincoid.me.service;

import com.zincoid.me.model.vo.MessageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MessageStreamService {

    SseEmitter subscribe();

    void broadcast(MessageVO message);

    void delete(Long messageId);

    void heartbeat();
}
