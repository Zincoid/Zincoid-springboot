package com.zincoid.me.ai.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.zincoid.me.ai.message.AiMessage;
import com.zincoid.me.ai.model.ModelReq;
import com.zincoid.me.ai.tool.ToolDef;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiReq {

    private String model;
    private List<Map<String, Object>> messages;
    private Thinking thinking;
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;
    private boolean stream;
    @JsonProperty("max_tokens")
    private int maxTokens;
    private double temperature;
    @JsonProperty("frequency_penalty")
    private double frequencyPenalty;
    @JsonProperty("presence_penalty")
    private double presencePenalty;
    private List<Tool> tools;

    @Data
    @Builder
    public static class Thinking {
        private String type;
    }

    @Data
    @Builder
    public static class Tool {
        private String type;
        private Function function;
    }

    @Data
    @Builder
    public static class Function {
        private String name;
        private String description;
        @JsonRawValue
        private String parameters;
    }

    public static OpenAiReq from(ModelReq req, String modelName) {
        OpenAiReqBuilder builder = OpenAiReq.builder()
                .model(modelName)
                .messages(req.getMessages().stream().map(AiMessage::toMap).toList())
                .thinking(Thinking.builder()
                        .type(req.isThinking() ? "enabled" : "disabled").build())
                .stream(false)
                .maxTokens(req.getMaxTokens())
                .temperature(0.8)
                .frequencyPenalty(0.3)
                .presencePenalty(0.2);
        if (req.isThinking())
            builder.reasoningEffort("high");

        List<ToolDef> toolDefs = req.getTools();
        if (toolDefs != null && !toolDefs.isEmpty()) {
            List<Tool> tools = new ArrayList<>();
            for (ToolDef td : toolDefs) {
                tools.add(Tool.builder()
                        .type("function")
                        .function(Function.builder()
                                .name(td.getName())
                                .description(td.getDescription())
                                .parameters(td.getParameters().toString())
                                .build())
                        .build());
            }
            builder.tools(tools);
        }
        return builder.build();
    }
}
