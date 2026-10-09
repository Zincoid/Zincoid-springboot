package com.zincoid.me.ai.tool;

import java.util.List;

public record ToolRes(String text, List<String> images, boolean error) {

    public static ToolRes of(String text) {
        return new ToolRes(text, List.of(), false);
    }

    public static ToolRes of(String text, List<String> images) {
        return new ToolRes(text, images != null ? images : List.of(), false);
    }

    public static ToolRes error(String text) {
        return new ToolRes(text, List.of(), true);
    }
}
