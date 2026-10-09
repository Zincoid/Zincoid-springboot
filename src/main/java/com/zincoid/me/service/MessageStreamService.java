package com.zincoid.me.service;

import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.ToolEventVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MessageStreamService {

    SseEmitter subscribe();

    void broadcast(MessageVO message);

    void delete(Long messageId);

    void tool(ToolEventVO event);

    void heartbeat();
}
