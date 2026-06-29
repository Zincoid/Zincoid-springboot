package com.zincoid.me.converter;

import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.ArticleCardVO;
import com.zincoid.me.model.vo.ArticleDetailVO;
import com.zincoid.me.model.vo.LikerVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ArticleConverter {

    ArticleConverter INSTANCE = Mappers.getMapper(ArticleConverter.class);

    @Mapping(target = "id", source = "article.id")
    @Mapping(target = "title", source = "article.title")
    @Mapping(target = "createdAt", source = "article.createdAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar")
    ArticleCardVO toCardVO(Article article, User user, boolean isLiked, long likeCount, int commentCount);

    @Mapping(target = "id", source = "article.id")
    @Mapping(target = "title", source = "article.title")
    @Mapping(target = "createdAt", source = "article.createdAt")
    @Mapping(target = "updatedAt", source = "article.updatedAt")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar")
    ArticleDetailVO toDetailVO(Article article, User user, boolean isLiked, long likeCount,
                               List<LikerVO> recentLikers);
}
