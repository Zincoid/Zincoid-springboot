package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.state.Role;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.enums.type.BizType;
import com.zincoid.me.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface FileService extends IService<File> {

    File get(String path);

    boolean exists(String filePathOrUrl);

    boolean accessible(File file, Long userId, Role role);

    boolean cacheable(File file);

    FileVO upload(Long userId, MultipartFile file);

    void _link(List<Long> fileIds, BizType relatedType, Long relatedId, Long userId);

    void link(List<String> filePathsOrUrls, BizType relatedType, Long relatedId, Long userId);

    void delete(Long fileId);

    void delete(String filePathOrUrl);

    void delete(BizType relatedType, Long relatedId);

    Map<String, Integer> cleanup(boolean isLogic);

    int cleanupUnlinked(Long userId);

    int cleanupCache();

    long totalSize(Long userId);

    Path logFile();
}
