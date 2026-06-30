package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.NotificationType;
import com.zincoid.me.model.po.Notification;
import com.zincoid.me.model.vo.NotificationVO;
import com.zincoid.me.model.vo.PageVO;

public interface NotificationService extends IService<Notification> {

    PageVO<NotificationVO> list(Long userId, int page, int size);

    void notify(Long senderId, Long receiverId, NotificationType type, Long relatedId);

    void notify(Long senderId, String content, NotificationType type, Long relatedId);

    void deleteOne(Long notificationId, Long userId);

    void deleteAll(NotificationType type, Long relatedId);

    void deleteAll(Long userId);

    void readOne(Long notificationId, Long userId);

    void readAll(Long userId);

    long countUnread(Long userId);

    void broadcast(Long senderId, String content);
}
