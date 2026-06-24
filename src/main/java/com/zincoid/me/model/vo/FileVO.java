package com.zincoid.me.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileVO {

    private String fileName;
    private String filePath;
    private String url;
    private Long fileSize;
}
