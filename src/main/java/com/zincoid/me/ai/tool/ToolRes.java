package com.zincoid.me.ai.tool;

import java.util.List;

public record ToolRes(String text, List<String> images) {

    public static ToolRes of(String text) {
        return new ToolRes(text, List.of());
    }

    public static ToolRes of(String text, List<String> images) {
        return new ToolRes(text, images != null ? images : List.of());
    }
}
