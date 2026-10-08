package com.zincoid.me.ai.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolReg {

    private final Map<String, Tool> toolMap = new LinkedHashMap<>();

    public ToolReg(List<Tool> tools) {
        tools.forEach(this::register);
    }

    public void register(Tool tool) {
        toolMap.put(tool.def().getName(), tool);
    }

    public Tool get(String name) {
        return toolMap.get(name);
    }

    public List<ToolDef> getAll() {
        return toolMap.values().stream()
                .map(Tool::def)
                .toList();
    }
}
