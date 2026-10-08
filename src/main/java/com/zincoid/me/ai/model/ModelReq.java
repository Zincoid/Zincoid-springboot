package com.zincoid.me.ai.model;

import com.zincoid.me.ai.message.AiMessage;
import com.zincoid.me.ai.tool.ToolDef;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class ModelReq {

    private final List<AiMessage> messages;
    private final List<ToolDef> tools;
    private final boolean thinking;
    private final int maxTokens;

    public static ModelReq of(List<AiMessage> messages, boolean thinking, int maxTokens) {
        return ModelReq.of(messages, null, thinking, maxTokens);
    }
}
