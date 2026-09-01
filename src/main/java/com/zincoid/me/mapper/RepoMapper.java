package com.zincoid.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.me.model.po.Repo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RepoMapper extends BaseMapper<Repo> {

    @Update("UPDATE repo SET view_count = view_count + 1, updated_at = updated_at WHERE id = #{id}")
    int addViewCount(Long id);
}
