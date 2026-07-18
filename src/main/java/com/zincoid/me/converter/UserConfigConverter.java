package com.zincoid.me.converter;

import com.zincoid.me.model.po.UserConfig;
import com.zincoid.me.model.vo.UserConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConfigConverter {

    UserConfigConverter INSTANCE = Mappers.getMapper(UserConfigConverter.class);

    UserConfigVO toVO(UserConfig userConfig);
}
