package com.zincoid.me.model.dto;

import com.zincoid.me.model.enums.Visibility;
import lombok.Data;

import java.util.List;

@Data
public class MomentUpdateRequest {

    private String content;
    private List<String> urls;
    private Visibility visibility;
}
