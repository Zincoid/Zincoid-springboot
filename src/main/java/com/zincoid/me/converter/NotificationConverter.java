package com.zincoid.me.converter;

import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Notification;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.NotificationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface NotificationConverter {

    NotificationConverter INSTANCE = Mappers.getMapper(NotificationConverter.class);

    @Mapping(target = "id", source = "notification.id")
    @Mapping(target = "createdAt", source = "notification.createdAt")
    @Mapping(target = "senderNickname", source = "sender.nickname")
    @Mapping(target = "senderAvatar", source = "sender.avatar")
    NotificationVO toVO(Notification notification, User sender, RelatedType targetType, Long targetId, String snippet);
}
