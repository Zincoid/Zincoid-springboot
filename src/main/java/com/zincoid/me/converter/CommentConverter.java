package com.zincoid.me.converter;

import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.utils.FileUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CommentConverter {

    CommentConverter INSTANCE = Mappers.getMapper(CommentConverter.class);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar", qualifiedByName = "thumbUrl")
    CommentVO toVO(Comment comment, User user, List<CommentVO> replies, long replyCount);

    @Named("thumbUrl")
    default String thumbUrl(String url) {
        return FileUtil.toThumbUrl(url);
    }
}
