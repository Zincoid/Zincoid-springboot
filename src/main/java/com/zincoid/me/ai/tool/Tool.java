package com.zincoid.me.ai.tool;

public interface Tool {

    ToolDef def();

    String run(String json);
}
