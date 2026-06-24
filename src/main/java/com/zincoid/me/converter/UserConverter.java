package com.zincoid.me.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.UserCardVO;
import com.zincoid.me.model.vo.UserDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    UserCardVO toCardVO(User user);

    @Mapping(target = "skills", source = "skills", qualifiedByName = "parseSkills")
    UserDetailVO toDetailVO(User user);

    @Named("parseSkills")
    default List<String> parseSkills(String skillsJson) {
        if (skillsJson == null || skillsJson.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(skillsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
