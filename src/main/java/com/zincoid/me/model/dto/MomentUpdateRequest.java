package com.zincoid.me.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MomentUpdateRequest {

    private String content;
    private List<String> images;
}
