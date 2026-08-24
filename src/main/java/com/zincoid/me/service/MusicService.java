package com.zincoid.me.service;

import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.model.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

public interface MusicService {

    FileVO upload(Long userId, MultipartFile file, boolean isPublic, boolean isAdmin);

    void delete(Long userId, Long fileId);

    PageVO<FileVO> list(int page, int size);

    PageVO<FileVO> list(Long userId, int page, int size);
}
