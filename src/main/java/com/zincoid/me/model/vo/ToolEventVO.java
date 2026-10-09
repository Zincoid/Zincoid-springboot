package com.zincoid.me.model.vo;

import com.zincoid.me.ai.client.AiTask;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolEventVO {

    private AiTask task;
    private String tcId;
    private String name;
    private String args;
    private String state;
    private String result;
}
