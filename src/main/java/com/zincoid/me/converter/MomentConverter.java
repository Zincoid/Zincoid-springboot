package com.zincoid.me.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.me.model.po.Moment;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.model.vo.MomentCardVO;
import com.zincoid.me.model.vo.MomentDetailVO;
import com.zincoid.me.utils.FileUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface MomentConverter {

    MomentConverter INSTANCE = Mappers.getMapper(MomentConverter.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "id", source = "moment.id")
    @Mapping(target = "createdAt", source = "moment.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar", qualifiedByName = "thumbUrl")
    @Mapping(target = "urls", source = "moment.urls", qualifiedByName = "parseUrls")
    @Mapping(target = "thumbs", source = "moment.urls", qualifiedByName = "parseThumbs")
    MomentCardVO toCardVO(Moment moment, User user, boolean isLiked, long likeCount, int commentCount);

    @Mapping(target = "id", source = "moment.id")
    @Mapping(target = "createdAt", source = "moment.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar", qualifiedByName = "thumbUrl")
    @Mapping(target = "urls", source = "moment.urls", qualifiedByName = "parseUrls")
    @Mapping(target = "thumbs", source = "moment.urls", qualifiedByName = "parseThumbs")
    MomentDetailVO toDetailVO(Moment moment, User user, boolean isLiked, long likeCount,
                              List<LikerVO> recentLikers);

    @Named("parseUrls")
    default List<String> parseUrls(String urlsJson) {
        if (urlsJson == null || urlsJson.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(urlsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    @Named("thumbUrl")
    default String thumbUrl(String url) {
        return FileUtil.toThumbUrl(url);
    }

    @Named("parseThumbs")
    default List<String> parseThumbs(String urlsJson) {
        return parseUrls(urlsJson).stream()
                .map(FileUtil::toThumbUrl)
                .collect(Collectors.toList());
    }
}
