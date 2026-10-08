package com.zincoid.me.ai.model;

import com.zincoid.me.ai.tool.ToolCall;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class ModelRes {

    private final List<ToolCall> tcs;
    private final String content;
    private final String reasoning;

    public boolean hasTcs() {
        return tcs != null && !tcs.isEmpty();
    }

    public static ModelRes of(String content, String reasoning) {
        return ModelRes.of(List.of(), content, reasoning);
    }

    public static ModelRes of(List<ToolCall> tcs, String reasoning) {
        return ModelRes.of(tcs, null, reasoning);
    }
}
