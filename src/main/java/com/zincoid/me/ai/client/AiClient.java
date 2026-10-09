package com.zincoid.me.ai.client;

import com.zincoid.me.ai.AiChatProperties;
import com.zincoid.me.ai.message.AiMessage;
import com.zincoid.me.ai.model.Model;
import com.zincoid.me.ai.model.ModelReq;
import com.zincoid.me.ai.model.ModelRes;
import com.zincoid.me.ai.tool.Tool;
import com.zincoid.me.ai.tool.ToolCall;
import com.zincoid.me.ai.tool.ToolReg;
import com.zincoid.me.ai.tool.ToolRes;
import com.zincoid.me.model.vo.ToolEventVO;
import com.zincoid.me.service.MessageStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final Model model;
    private final ToolReg toolReg;
    private final AiChatProperties aiChatProperties;
    private final MessageStreamService messageStreamService;

    public String invoke(List<AiMessage> history, String prompt, boolean thinking, AiTask task) {
        List<AiMessage> work = new ArrayList<>();
        if (prompt != null && !prompt.isBlank())
            work.add(AiMessage.system(prompt));
        work.addAll(history);
        int maxTcs = aiChatProperties.getMaxTcs();
        int maxTokens = aiChatProperties.getMaxTokens();
        if (maxTcs <= 0)
            return replyFinal(work, thinking, maxTokens);
        for (int i = 0; i < maxTcs; i++) {
            ModelRes res = model.invoke(ModelReq.of(
                    work,
                    toolReg.getAll(),
                    thinking,
                    maxTokens
            ));
            if (!res.hasTcs())
                return ensureContent(res.getContent());
            log.info("Tc round {}: {} call(s)", i + 1, res.getTcs().size());
            work.add(AiMessage.assistant(res.getTcs())
                    .withReasoning(res.getReasoning()));
            List<AiMessage> images = new ArrayList<>();
            for (ToolCall tc : res.getTcs()) {
                streamTool(task, AiState.RUNNING, tc, null);
                ToolRes tr = runTool(tc);
                streamTool(task, AiState.DONE, tc, truncateContent(tr.text()));
                work.add(AiMessage.tool(tc.getId(), tr.text()));
                if (tr.images().isEmpty()) continue;
                images.add(AiMessage.user(
                        null,
                        "Image(s) from tool \"%s\"".formatted(tc.getName()),
                        tr.images()
                ));
            }
            work.addAll(images);
        }
        log.warn("Tc limit reached: {}", maxTcs);
        work.add(AiMessage.user("""
                The maximum number of tool call rounds has been reached.
                Please provide the final answer based on the available information.
                Do not call any more tools!!!"""
        ));
        return replyFinal(work, thinking, maxTokens);
    }

    // ──────── Private tool ────────────────────────────────

    private String replyFinal(List<AiMessage> work, boolean thinking, int maxTokens) {
        ModelRes res = model.invoke(ModelReq.of(work, thinking, maxTokens));
        return ensureContent(res.getContent());
    }

    private String ensureContent(String content) {
        if (content != null && !content.isBlank()) return content;
        return "(AI is temporarily unable to answer)";
    }

    private String truncateContent(String content) {
        if (content == null) return null;
        return content.length() > 200 ? "%s...(truncated)".formatted(content.substring(0, 200)) : content;
    }

    private void streamTool(AiTask task, AiState state, ToolCall tc, String result) {
        messageStreamService.tool(ToolEventVO.builder()
                .task(task)
                .state(state)
                .tcId(tc.getId())
                .name(tc.getName())
                .args(tc.getArgs())
                .result(result)
                .build());
    }

    private ToolRes runTool(ToolCall tc) {
        Tool tool = toolReg.get(tc.getName());
        if (tool == null)
            return ToolRes.of("Error: Tool \"%s\" doesn't exist."
                    .formatted(tc.getName()));
        try {
            ToolRes res = tool.run(tc.getArgs());
            log.info("Tool success: {}({}) -> {}", tc.getName(), tc.getArgs(), res);
            return res;
        } catch (Exception e) {
            log.warn("Tool error: {}", e.getMessage());
            return ToolRes.of("Error: %s.".formatted(e.getMessage()));
        }
    }
}
