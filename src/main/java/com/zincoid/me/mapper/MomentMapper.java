package com.zincoid.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.me.model.po.Moment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MomentMapper extends BaseMapper<Moment> {

    @Update("UPDATE moment SET view_count = view_count + 1, updated_at = updated_at WHERE id = #{id}")
    int addViewCount(Long id);
}
