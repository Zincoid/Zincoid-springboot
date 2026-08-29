package com.zincoid.me.converter;

import com.zincoid.me.model.enums.FileType;
import com.zincoid.me.model.po.File;
import com.zincoid.me.model.po.Repo;
import com.zincoid.me.model.po.RepoItem;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.vo.GitHubRepoVO;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.model.vo.RepoCardVO;
import com.zincoid.me.model.vo.RepoDetailVO;
import com.zincoid.me.model.vo.RepoItemVO;
import com.zincoid.me.utils.FileUtil;
import com.zincoid.me.utils.JsonUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RepoConverter {

    RepoConverter INSTANCE = Mappers.getMapper(RepoConverter.class);

    @Mapping(target = "id", source = "repo.id")
    @Mapping(target = "userId", source = "repo.userId")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar", qualifiedByName = "thumbUrl")
    @Mapping(target = "name", source = "repo.name")
    @Mapping(target = "description", source = "repo.description")
    @Mapping(target = "type", source = "repo.type")
    @Mapping(target = "visibility", source = "repo.visibility")
    @Mapping(target = "url", source = "repo.url")
    @Mapping(target = "tags", source = "repo.tags", qualifiedByName = "parseTags")
    @Mapping(target = "coverThumb", source = "cover", qualifiedByName = "thumbUrl")
    @Mapping(target = "viewCount", source = "repo.viewCount")
    @Mapping(target = "likeCount", source = "likeCount")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "itemCount", source = "itemCount")
    @Mapping(target = "isLiked", source = "isLiked")
    @Mapping(target = "restricted", source = "isRestricted")
    @Mapping(target = "createdAt", source = "repo.createdAt")
    RepoCardVO toCardVO(Repo repo, User user, boolean isLiked,
                        long likeCount, long commentCount, long itemCount,
                        boolean isRestricted, String cover);

    @Mapping(target = "id", source = "repo.id")
    @Mapping(target = "userId", source = "repo.userId")
    @Mapping(target = "userNickname", source = "user.nickname")
    @Mapping(target = "userAvatar", source = "user.avatar", qualifiedByName = "thumbUrl")
    @Mapping(target = "name", source = "repo.name")
    @Mapping(target = "description", source = "repo.description")
    @Mapping(target = "type", source = "repo.type")
    @Mapping(target = "visibility", source = "repo.visibility")
    @Mapping(target = "url", source = "repo.url")
    @Mapping(target = "tags", source = "repo.tags", qualifiedByName = "parseTags")
    @Mapping(target = "coverImage", source = "cover")
    @Mapping(target = "isDefaultCover", source = "isDefaultCover")
    @Mapping(target = "viewCount", source = "repo.viewCount")
    @Mapping(target = "likeCount", source = "likeCount")
    @Mapping(target = "isLiked", source = "isLiked")
    @Mapping(target = "recentLikers", source = "recentLikers")
    @Mapping(target = "github", source = "github")
    @Mapping(target = "restricted", ignore = true)
    @Mapping(target = "createdAt", source = "repo.createdAt")
    @Mapping(target = "updatedAt", source = "repo.updatedAt")
    RepoDetailVO toDetailVO(Repo repo, User user, boolean isLiked, long likeCount,
                            List<LikerVO> recentLikers, GitHubRepoVO github,
                            boolean isDefaultCover, String cover);

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "sortOrder", source = "item.sortOrder")
    @Mapping(target = "fileId", source = "item.fileId")
    @Mapping(target = "name", source = "item.name")
    @Mapping(target = "url", source = "file", qualifiedByName = "urlOfFile")
    @Mapping(target = "thumb", source = "file", qualifiedByName = "thumbOfFile")
    @Mapping(target = "fileSize", source = "file", qualifiedByName = "sizeOfFile")
    @Mapping(target = "createdAt", source = "item.createdAt")
    RepoItemVO toItemVO(RepoItem item, File file);

    @Named("thumbUrl")
    default String thumbUrl(String url) {
        return FileUtil.toThumbUrl(url);
    }

    @Named("parseTags")
    default List<String> parseTags(String tagsJson) {
        return JsonUtil.parseImages(tagsJson);
    }

    @Named("urlOfFile")
    default String urlOfFile(File file) {
        return file == null ? null : "/uploads/" + file.getFilePath();
    }

    @Named("thumbOfFile")
    default String thumbOfFile(File file) {
        if (file == null || file.getFileType() != FileType.IMAGE) return null;
        return FileUtil.toThumbUrl(urlOfFile(file));
    }

    @Named("sizeOfFile")
    default Long sizeOfFile(File file) {
        return file == null ? null : file.getFileSize();
    }
}