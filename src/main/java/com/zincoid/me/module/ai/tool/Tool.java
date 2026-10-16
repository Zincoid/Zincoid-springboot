package com.zincoid.me.module.ai.tool;

public interface Tool {

    ToolDef def();

    ToolRes run(String json);
}
