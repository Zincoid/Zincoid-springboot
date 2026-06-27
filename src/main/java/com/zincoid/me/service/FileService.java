package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService extends IService<File> {

    FileVO upload(Long userId, MultipartFile file, RelatedType relatedType, Long relatedId);

    void link(List<String> filePathsOrUrls, RelatedType relatedType, Long relatedId);

    void delete(String filePathOrUrl);

    void delete(RelatedType relatedType, Long relatedId);

    void cleanup();
}
