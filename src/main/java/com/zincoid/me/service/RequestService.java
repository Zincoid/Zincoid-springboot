package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.Access;
import com.zincoid.me.model.enums.RequestType;
import com.zincoid.me.model.po.Request;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.model.vo.RequestVO;

public interface RequestService extends IService<Request> {

    RequestVO create(Long senderId, Long receiverId, RequestType type, String content);

    PageVO<RequestVO> sent(Long userId, int page, int size);

    PageVO<RequestVO> received(Long userId, int page, int size, boolean isAdmin);

    RequestVO handle(Long userId, Long requestId, Access access, boolean isAdmin);
}
