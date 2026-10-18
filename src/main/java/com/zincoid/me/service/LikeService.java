package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.BizType;
import com.zincoid.me.model.po.Like;
import com.zincoid.me.model.vo.LikerVO;

import java.util.List;

public interface LikeService extends IService<Like> {

    boolean liked(Long userId, BizType targetType, Long targetId);

    boolean toggle(Long userId, BizType targetType, Long targetId);

    long count(BizType targetType, Long targetId);

    void delete(BizType targetType, Long targetId);

    List<LikerVO> getLikers(BizType targetType, Long targetId, int limit);
}
