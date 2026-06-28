package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.model.vo.PageVO;

public interface MessageService extends IService<Message> {

    MessageVO send(Long userId, String content, String file);

    PageVO<MessageVO> list(int page, int size);
}
