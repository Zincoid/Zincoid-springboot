package com.zincoid.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.me.model.po.RepoItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RepoItemMapper extends BaseMapper<RepoItem> {

    @Select("""
            SELECT f.file_path
            FROM repo_item i
            JOIN file f ON f.id = i.file_id AND f.file_type = 0
            WHERE i.repo_id = #{repoId}
            ORDER BY i.sort_order
            LIMIT 1
            """)
    String selectFirstImagePath(Long repoId);
}
