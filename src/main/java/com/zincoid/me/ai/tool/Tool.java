package com.zincoid.me.ai.tool;

public interface Tool {

    ToolDef def();

    ToolRes run(String json);
}
