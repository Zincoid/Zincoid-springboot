package com.zincoid.me.service;

import com.zincoid.me.module.ai.client.AiTask;
import com.zincoid.me.module.ai.tool.ToolCall;
import com.zincoid.me.module.ai.tool.ToolRes;
import com.zincoid.me.model.vo.MessageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MessageStreamService {

    SseEmitter subscribe();

    void broadcast(MessageVO message);

    void delete(Long messageId);

    void trim(Long messageId);

    void tool(AiTask task, ToolCall tc);

    void tool(AiTask task, ToolCall tc, ToolRes tr);

    void heartbeat();
}
