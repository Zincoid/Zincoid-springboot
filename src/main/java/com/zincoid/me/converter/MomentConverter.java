package com.zincoid.me.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.model.vo.MomentCardVO;
import com.zincoid.me.model.vo.MomentDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MomentConverter {

    MomentConverter INSTANCE = Mappers.getMapper(MomentConverter.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "id", source = "moment.id")
    @Mapping(target = "createdAt", source = "moment.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar")
    @Mapping(target = "images", source = "moment.images", qualifiedByName = "parseImages")
    MomentCardVO toCardVO(Moment moment, User user, boolean isLiked, long likeCount, int commentCount);

    @Mapping(target = "id", source = "moment.id")
    @Mapping(target = "createdAt", source = "moment.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar")
    @Mapping(target = "images", source = "moment.images", qualifiedByName = "parseImages")
    MomentDetailVO toDetailVO(Moment moment, User user, boolean isLiked, long likeCount,
                              List<LikerVO> recentLikers);

    @Named("parseImages")
    default List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
