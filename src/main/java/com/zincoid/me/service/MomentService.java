package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.dto.MomentCreateRequest;
import com.zincoid.me.model.dto.MomentUpdateRequest;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.vo.MomentCardVO;
import com.zincoid.me.model.vo.MomentDetailVO;
import com.zincoid.me.model.vo.PageVO;

public interface MomentService extends IService<Moment> {

    MomentCardVO create(Long userId, MomentCreateRequest request);

    MomentCardVO update(Long userId, Long momentId, MomentUpdateRequest request);

    void delete(Long userId, Long momentId, boolean isAdmin);

    void pin(Long momentId);

    void unpin(Long momentId);

    PageVO<MomentCardVO> list(int page, int size);

    PageVO<MomentCardVO> list(Long userId, int page, int size);

    MomentDetailVO get(Long momentId);

    MomentCardVO random();
}
