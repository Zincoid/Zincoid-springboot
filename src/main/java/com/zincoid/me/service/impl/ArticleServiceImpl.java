package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.model.vo.PageVO;
import com.zincoid.me.converter.ArticleConverter;
import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.mapper.ArticleMapper;
import com.zincoid.me.model.dto.ArticleCreateRequest;
import com.zincoid.me.model.dto.ArticleUpdateRequest;
import com.zincoid.me.model.po.Article;
import com.zincoid.me.model.po.User;
import com.zincoid.me.model.enums.RelatedType;
import com.zincoid.me.model.enums.Status;
import com.zincoid.me.model.vo.ArticleCardVO;
import com.zincoid.me.model.vo.ArticleDetailVO;
import com.zincoid.me.model.vo.CommentVO;
import com.zincoid.me.model.vo.LikerVO;
import com.zincoid.me.service.ArticleService;
import com.zincoid.me.service.CommentService;
import com.zincoid.me.service.FileService;
import com.zincoid.me.service.LikeService;
import com.zincoid.me.service.UserService;
import com.zincoid.me.utils.MdTool;
import com.zincoid.me.utils.AuthCtx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\((/uploads/[^\\s)]+)\\)");

    private final UserService userService;
    private final CommentService commentService;
    private final FileService fileService;
    private final LikeService likeService;
    private final MdTool mdTool;

    public ArticleServiceImpl(@Lazy UserService userService, CommentService commentService,
                              FileService fileService, LikeService likeService, MdTool mdTool) {
        this.userService = userService;
        this.commentService = commentService;
        this.fileService = fileService;
        this.likeService = likeService;
        this.mdTool = mdTool;
    }

    @Override
    @Transactional
    public ArticleDetailVO create(Long userId, ArticleCreateRequest request) {
        Article article = Article.builder()
                .userId(userId)
                .title(request.getTitle())
                .contentMd(request.getContentMd())
                .contentHtml(mdTool.renderToHtml(request.getContentMd()))
                .summary(request.getSummary())
                .coverImage(request.getCoverImage())
                .build();
        save(article);
        List<String> urls = new ArrayList<>();
        if (article.getCoverImage() != null) urls.add(article.getCoverImage());
        urls.addAll(extractUploadUrls(article.getContentMd()));
        if (!urls.isEmpty()) fileService.link(urls, RelatedType.ARTICLE, article.getId());
        log.info("Article created: user={}, id={}", userId, article.getId());
        return buildDetailVO(article);
    }

    @Override
    @Transactional
    public ArticleDetailVO update(Long userId, Long articleId, ArticleUpdateRequest request) {
        Article article = getById(articleId);
        if (article == null)
            throw new BusinessException(404, "Article not found");
        if (!article.getUserId().equals(userId))
            throw new BusinessException(403, "You can only edit your own articles");
        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContentMd() != null) {
            Set<String> oldUrls = new HashSet<>(extractUploadUrls(article.getContentMd()));
            article.setContentMd(request.getContentMd());
            article.setContentHtml(mdTool.renderToHtml(request.getContentMd()));
            Set<String> newUrls = new HashSet<>(extractUploadUrls(article.getContentMd()));
            for (String oldUrl : oldUrls)
                if (!newUrls.contains(oldUrl)) fileService.delete(oldUrl);
        }
        if (request.getSummary() != null) article.setSummary(request.getSummary());
        if (request.getCoverImage() != null) {
            String newCover = request.getCoverImage().isBlank() ? null : request.getCoverImage();
            String oldCover = article.getCoverImage();
            article.setCoverImage(newCover);
            if (newCover == null)
                lambdaUpdate().set(Article::getCoverImage, null).eq(Article::getId, articleId).update();
            if (oldCover != null && !oldCover.equals(newCover))
                fileService.delete(oldCover);
        }
        if (request.getStatus() != null) article.setStatus(request.getStatus());
        updateById(article);
        List<String> urls = new ArrayList<>();
        if (request.getCoverImage() != null && !request.getCoverImage().isBlank())
            urls.add(request.getCoverImage());
        urls.addAll(extractUploadUrls(article.getContentMd()));
        if (!urls.isEmpty()) fileService.link(urls, RelatedType.ARTICLE, article.getId());
        log.info("Article updated: user={}, id={}", userId, articleId);
        return buildDetailVO(article);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long articleId, boolean isAdmin) {
        Article article = getById(articleId);
        if (article == null)
            throw new BusinessException(404, "Article not found");
        if (!isAdmin && !article.getUserId().equals(userId))
            throw new BusinessException(403, "No permission to delete this article");
        likeService.delete(RelatedType.ARTICLE, articleId);
        commentService.delete(RelatedType.ARTICLE, articleId);
        fileService.delete(RelatedType.ARTICLE, articleId);
        removeById(articleId);
        log.info("Article deleted: user={}, admin={}, id={}", userId, isAdmin, articleId);
    }

    @Override
    public void pin(Long articleId) {
        Article article = getById(articleId);
        if (article == null)
            throw new BusinessException(404, "Article not found");
        article.setIsPinned(true);
        updateById(article);
        log.info("Article pinned: id={}", articleId);
    }

    @Override
    public void unpin(Long articleId) {
        Article article = getById(articleId);
        if (article == null)
            throw new BusinessException(404, "Article not found");
        article.setIsPinned(false);
        updateById(article);
        log.info("Article unpinned: id={}", articleId);
    }

    @Override
    public ArticleCardVO random() {
        Article article = lambdaQuery()
                .eq(Article::getStatus, Status.ACTIVE)
                .last("ORDER BY RAND() LIMIT 1")
                .one();
        if (article == null) return null;
        return buildCardVO(article);
    }

    @Override
    public PageVO<ArticleCardVO> list(int page, int size, boolean pinned) {
        Page<Article> articlePage = lambdaQuery()
                .eq(Article::getStatus, Status.ACTIVE)
                .orderByDesc(pinned, Article::getIsPinned)
                .orderByDesc(Article::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(articlePage, this::buildCardVO);
    }

    @Override
    public List<ArticleCardVO> home(int size) {
        List<Article> pinned = lambdaQuery()
                .eq(Article::getStatus, Status.ACTIVE)
                .eq(Article::getIsPinned, true)
                .orderByDesc(Article::getCreatedAt)
                .list();
        List<Article> records = new ArrayList<>(pinned);
        List<Article> nonPinned = lambdaQuery()
                .eq(Article::getStatus, Status.ACTIVE)
                .eq(Article::getIsPinned, false)
                .orderByDesc(Article::getCreatedAt)
                .last("LIMIT " + size)
                .list();
        records.addAll(nonPinned);
        return records.stream().map(this::buildCardVO).toList();
    }

    @Override
    public PageVO<ArticleCardVO> list(Long userId, int page, int size, boolean pinned) {
        Page<Article> articlePage = lambdaQuery()
                .eq(Article::getUserId, userId)
                .eq(Article::getStatus, Status.ACTIVE)
                .orderByDesc(pinned, Article::getIsPinned)
                .orderByDesc(Article::getCreatedAt)
                .page(Page.of(page, size));
        return PageVO.of(articlePage, this::buildCardVO);
    }

    @Override
    public ArticleDetailVO get(Long articleId) {
        Article article = getById(articleId);
        if (article == null)
            throw new BusinessException(404, "Article not found");
        baseMapper.addViewCount(articleId);
        article.setViewCount(article.getViewCount() + 1);
        return buildDetailVO(article);
    }

    // ──────── Private tool ────────────────────────────────

    private List<String> extractUploadUrls(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<String> paths = new ArrayList<>();
        Matcher m = MD_IMAGE_PATTERN.matcher(content);
        while (m.find()) paths.add(m.group(1));
        return paths;
    }

    private ArticleCardVO buildCardVO(Article article) {
        User user = userService.getById(article.getUserId());
        int commentCount = (int) commentService.count(RelatedType.ARTICLE, article.getId());
        long likeCount = likeService.count(RelatedType.ARTICLE, article.getId());
        boolean isLiked = likeService.liked(AuthCtx.getUserId(), RelatedType.ARTICLE, article.getId());
        return ArticleConverter.INSTANCE.toCardVO(article, user, isLiked, likeCount, commentCount);
    }

    private ArticleDetailVO buildDetailVO(Article article) {
        User user = userService.getById(article.getUserId());
        long likeCount = likeService.count(RelatedType.ARTICLE, article.getId());
        boolean isLiked = likeService.liked(AuthCtx.getUserId(), RelatedType.ARTICLE, article.getId());
        List<LikerVO> recentLikers = likeService.getLikers(RelatedType.ARTICLE, article.getId(), 5);
        return ArticleConverter.INSTANCE.toDetailVO(article, user, isLiked, likeCount, recentLikers);
    }
}
