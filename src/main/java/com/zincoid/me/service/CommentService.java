package com.zincoid.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.po.Comment;
import com.zincoid.me.model.vo.CommentVO;

import java.util.List;

public interface CommentService extends IService<Comment> {

    List<CommentVO> list(RelatedType targetType, Long targetId);

    long count(RelatedType targetType, Long targetId);

    CommentVO add(Long userId, RelatedType targetType, Long targetId, String content, Long parentId);

    void delete(Long userId, Long commentId, boolean isAdmin);

    void delete(RelatedType targetType, Long targetId);
}
