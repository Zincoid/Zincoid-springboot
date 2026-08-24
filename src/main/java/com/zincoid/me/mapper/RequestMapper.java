package com.zincoid.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.me.model.po.Request;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RequestMapper extends BaseMapper<Request> {
}
