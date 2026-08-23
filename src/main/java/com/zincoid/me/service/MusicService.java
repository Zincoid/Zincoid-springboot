package com.zincoid.me.service;

import com.zincoid.me.model.vo.FileVO;
import com.zincoid.me.model.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

public interface MusicService {

    FileVO upload(Long userId, MultipartFile file);

    void delete(Long fileId);

    PageVO<FileVO> list(int page, int size);
}
