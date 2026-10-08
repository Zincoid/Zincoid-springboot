package com.zincoid.me.ai.model.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zincoid.me.ai.model.ModelRes;
import com.zincoid.me.ai.tool.ToolCall;
import com.zincoid.me.exception.BusinessException;

import java.util.List;
import java.util.Optional;

public record OpenAiRes(List<Choice> choices) {

    public record Choice(Message message) {}
    public record Message(String content,
                          @JsonProperty("reasoning_content") String reasoning,
                          @JsonProperty("tool_calls") List<ToolCallNode> tcs) {}
    public record ToolCallNode(String id, Function function) {}
    public record Function(String name, String arguments) {}

    public ModelRes toModelRes() {
        Message msg = Optional.ofNullable(choices)
                .filter(c -> !c.isEmpty())
                .map(c -> c.getFirst().message)
                .orElse(null);
        if (msg == null)
            throw new BusinessException(502, "No available message in AI response");
        String content = msg.content != null ? msg.content : "";
        String reasoning = msg.reasoning != null ? msg.reasoning : "";
        if (msg.tcs == null || msg.tcs.isEmpty()) {
            if (content.isBlank() && !reasoning.isBlank())
                throw new BusinessException(502, "Request too complex, reasoning was interrupted");
            return ModelRes.of(content, reasoning);
        }
        List<ToolCall> calls = msg.tcs.stream()
                .map(tc -> new ToolCall(tc.id, tc.function.name, tc.function.arguments))
                .toList();
        return ModelRes.of(calls, reasoning);
    }
}
