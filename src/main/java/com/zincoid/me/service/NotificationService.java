package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Notification;
import com.zincoid.me.model.vo.NotificationVO;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    void add(Long senderId, Long receiverId, RelatedType relatedType, Long relatedId, Long commentId);

    void deleteByCommentId(Long commentId);

    void deleteByUserId(Long userId);

    void deleteOne(Long notificationId, Long userId);

    List<NotificationVO> list(Long userId);

    void readAll(Long userId);

    long countUnread(Long userId);
}
