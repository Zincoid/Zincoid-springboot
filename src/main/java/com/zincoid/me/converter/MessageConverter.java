package com.zincoid.me.converter;

import com.zincoid.me.model.po.Message;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.MessageVO;
import com.zincoid.me.utils.FileUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MessageConverter {

    MessageConverter INSTANCE = Mappers.getMapper(MessageConverter.class);

    @Mapping(target = "id", source = "message.id")
    @Mapping(target = "createdAt", source = "message.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar", qualifiedByName = "thumbUrl")
    MessageVO toVO(Message message, User user);

    @Named("thumbUrl")
    default String thumbUrl(String url) {
        return FileUtil.toThumbUrl(url);
    }
}
