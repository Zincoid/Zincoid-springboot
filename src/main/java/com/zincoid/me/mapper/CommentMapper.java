package com.zincoid.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("""
            SELECT c.*
            FROM comment c
            WHERE c.target_type = #{targetType} AND c.target_id = #{targetId} AND c.parent_id IS NULL
            ORDER BY c.is_pinned DESC, (SELECT COUNT(*) FROM likes l WHERE l.target_type = #{likeTargetType} AND l.target_id = c.id) DESC, c.created_at DESC
            """)
    IPage<Comment> selectRootsByLikes(Page<Comment> page,
                                      @Param("targetType") RelatedType targetType,
                                      @Param("targetId") Long targetId,
                                      @Param("likeTargetType") RelatedType likeTargetType);
}
