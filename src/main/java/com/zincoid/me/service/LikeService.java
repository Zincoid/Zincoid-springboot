package com.zincoid.me.service;

import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.vo.LikerVO;

import java.util.List;

public interface LikeService {

    boolean liked(Long userId, RelatedType targetType, Long targetId);

    boolean toggle(Long userId, RelatedType targetType, Long targetId);

    long count(RelatedType targetType, Long targetId);

    void delete(RelatedType targetType, Long targetId);

    List<LikerVO> getLikers(RelatedType targetType, Long targetId, int limit);
}
