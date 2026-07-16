package com.ruoyi.blog.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogArticle;
import com.ruoyi.blog.domain.BlogCategory;
import com.ruoyi.blog.domain.BlogComment;
import com.ruoyi.blog.domain.BlogTag;
import com.ruoyi.blog.dto.ArticlePageQuery;
import com.ruoyi.blog.dto.ArticleSaveRequest;
import com.ruoyi.blog.dto.ArticleTagRow;
import com.ruoyi.blog.mapper.BlogArticleMapper;
import com.ruoyi.blog.mapper.BlogArticleTagMapper;
import com.ruoyi.blog.mapper.BlogCategoryMapper;
import com.ruoyi.blog.mapper.BlogCommentMapper;
import com.ruoyi.blog.constant.BlogAnalyticsConstants;
import com.ruoyi.blog.service.BlogArticleService;
import com.ruoyi.blog.service.BlogVisitService;
import com.ruoyi.blog.service.BlogTagService;
import com.ruoyi.blog.vo.ArticleBriefVO;
import com.ruoyi.blog.vo.ArticleVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl implements BlogArticleService
{

    private static final int STATUS_PUBLISHED = 1;

    private final BlogArticleMapper blogArticleMapper;
    private final BlogVisitService blogVisitService;

    private final BlogCategoryMapper blogCategoryMapper;

    private final BlogArticleTagMapper blogArticleTagMapper;

    private final BlogTagService blogTagService;

    private final BlogCommentMapper blogCommentMapper;

    @Override
    public Page<ArticleVO> page(ArticlePageQuery query)
    {
        Page<BlogArticle> result = queryArticles(query, null);
        Map<Long, String> categoryMap = loadCategoryMap(result.getRecords());
        Map<Long, List<BlogTag>> tagsMap = loadTagsMap(result.getRecords());
        Map<Long, Long> commentCountMap = loadCommentCountMap(result.getRecords());
        Page<ArticleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(article -> toVO(article, categoryMap, tagsMap, commentCountMap))
                .toList());
        return voPage;
    }

    @Override
    public Page<ArticleBriefVO> publicPage(ArticlePageQuery query)
    {
        Page<BlogArticle> result = queryArticles(query, STATUS_PUBLISHED);
        Map<Long, String> categoryMap = loadCategoryMap(result.getRecords());
        Map<Long, List<BlogTag>> tagsMap = loadTagsMap(result.getRecords());
        Page<ArticleBriefVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(article -> toBriefVO(article, categoryMap, tagsMap)).toList());
        return voPage;
    }

    @Override
    public ArticleVO getById(Long id)
    {
        BlogArticle article = requireArticle(id);
        Map<Long, String> categoryMap = loadCategoryMap(List.of(article));
        Map<Long, List<BlogTag>> tagsMap = loadTagsMap(List.of(article));
        Map<Long, Long> commentCountMap = loadCommentCountMap(List.of(article));
        return toVO(article, categoryMap, tagsMap, commentCountMap);
    }

    @Override
    @Transactional
    public ArticleVO getPublishedById(Long id)
    {
        BlogArticle article = requireArticle(id);
        if (article.getStatus() == null || article.getStatus() != STATUS_PUBLISHED)
        {
            throw new ServiceException("资源不存在", HttpStatus.NOT_FOUND);
        }
        blogArticleMapper.incrementViewCount(id);
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        blogVisitService.recordFromRequest(BlogAnalyticsConstants.PAGE_ARTICLE, id);
        Map<Long, String> categoryMap = loadCategoryMap(List.of(article));
        Map<Long, List<BlogTag>> tagsMap = loadTagsMap(List.of(article));
        Map<Long, Long> commentCountMap = loadCommentCountMap(List.of(article));
        return toVO(article, categoryMap, tagsMap, commentCountMap);
    }

    @Override
    @Transactional
    public Long save(ArticleSaveRequest request)
    {
        BlogArticle article = new BlogArticle();
        if (request.getId() != null)
        {
            BlogArticle existing = blogArticleMapper.selectById(request.getId());
            if (existing == null)
            {
                throw new ServiceException("资源不存在", HttpStatus.NOT_FOUND);
            }
            article.setId(request.getId());
            article.setViewCount(existing.getViewCount());
            article.setIsAiGenerated(existing.getIsAiGenerated());
        }
        else
        {
            article.setViewCount(0);
            article.setIsAiGenerated(0);
        }
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setCategoryId(request.getCategoryId());
        article.setStatus(request.getStatus() != null ? request.getStatus() : 0);

        if (article.getId() == null)
        {
            blogArticleMapper.insert(article);
        }
        else
        {
            blogArticleMapper.updateById(article);
        }

        List<Long> tagIds = blogTagService.resolveTagIds(request.getTagIds(), request.getTagNames());
        bindTags(article.getId(), tagIds);
        return article.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null)
        {
            throw new ServiceException("资源不存在", HttpStatus.NOT_FOUND);
        }
        blogArticleMapper.deleteById(id);
    }

    @Override
    public Page<ArticleVO> recyclePage(ArticlePageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        long total = blogArticleMapper.countRecycle(keyword);
        long offset = (long) (pageNum - 1) * pageSize;
        List<BlogArticle> records = total == 0
                ? List.of()
                : blogArticleMapper.selectRecycleList(keyword, offset, pageSize);
        Map<Long, String> categoryMap = loadCategoryMap(records);
        Map<Long, List<BlogTag>> tagsMap = loadTagsMap(records);
        Page<ArticleVO> voPage = new Page<>(pageNum, pageSize, total);
        Map<Long, Long> commentCountMap = loadCommentCountMap(records);
        voPage.setRecords(records.stream()
                .map(article -> toVO(article, categoryMap, tagsMap, commentCountMap))
                .toList());
        return voPage;
    }

    @Override
    @Transactional
    public void restore(Long id)
    {
        BlogArticle article = blogArticleMapper.selectDeletedById(id);
        if (article == null)
        {
            throw new ServiceException("回收站中不存在该文章", HttpStatus.NOT_FOUND);
        }
        if (blogArticleMapper.restoreById(id) == 0)
        {
            throw new ServiceException("恢复失败", HttpStatus.ERROR);
        }
    }

    @Override
    @Transactional
    public void purge(Long id)
    {
        BlogArticle article = blogArticleMapper.selectDeletedById(id);
        if (article == null)
        {
            throw new ServiceException("回收站中不存在该文章", HttpStatus.NOT_FOUND);
        }
        blogArticleTagMapper.deleteByArticleId(id);
        if (blogArticleMapper.purgeById(id) == 0)
        {
            throw new ServiceException("彻底删除失败", HttpStatus.ERROR);
        }
    }

    private Page<BlogArticle> queryArticles(ArticlePageQuery query, Integer forceStatus)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<BlogArticle> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.and(w -> w.like(BlogArticle::getTitle, query.getKeyword()).or().like(BlogArticle::getSummary, query.getKeyword()));
        }
        Integer status = forceStatus != null ? forceStatus : query.getStatus();
        if (status != null)
        {
            wrapper.eq(BlogArticle::getStatus, status);
        }
        if (query.getCategoryId() != null)
        {
            wrapper.eq(BlogArticle::getCategoryId, query.getCategoryId());
        }
        if ("hot".equalsIgnoreCase(query.getSort()))
        {
            wrapper.orderByDesc(BlogArticle::getViewCount).orderByDesc(BlogArticle::getUpdateTime);
        }
        else
        {
            wrapper.orderByDesc(BlogArticle::getUpdateTime);
        }
        return blogArticleMapper.selectPage(page, wrapper);
    }

    private BlogArticle requireArticle(Long id)
    {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null)
        {
            throw new ServiceException("资源不存在", HttpStatus.NOT_FOUND);
        }
        return article;
    }

    private void bindTags(Long articleId, List<Long> tagIds)
    {
        blogArticleTagMapper.deleteByArticleId(articleId);
        if (!CollectionUtils.isEmpty(tagIds))
        {
            blogArticleTagMapper.batchInsert(articleId, tagIds);
        }
    }

    private Map<Long, String> loadCategoryMap(List<BlogArticle> articles)
    {
        List<Long> categoryIds = articles.stream().map(BlogArticle::getCategoryId).filter(id -> id != null).distinct().toList();
        if (categoryIds.isEmpty())
        {
            return Map.of();
        }
        return blogCategoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>().in(BlogCategory::getId, categoryIds)).stream()
                .collect(Collectors.toMap(BlogCategory::getId, BlogCategory::getName));
    }

    private Map<Long, List<BlogTag>> loadTagsMap(List<BlogArticle> articles)
    {
        if (CollectionUtils.isEmpty(articles))
        {
            return Map.of();
        }
        List<Long> articleIds = articles.stream().map(BlogArticle::getId).filter(id -> id != null).toList();
        if (articleIds.isEmpty())
        {
            return Map.of();
        }
        List<ArticleTagRow> rows = blogArticleTagMapper.selectTagsByArticleIds(articleIds);
        Map<Long, List<BlogTag>> map = new HashMap<>();
        for (ArticleTagRow row : rows)
        {
            BlogTag tag = new BlogTag();
            tag.setId(row.getId());
            tag.setName(row.getName());
            tag.setCreateTime(row.getCreateTime());
            map.computeIfAbsent(row.getArticleId(), k -> new ArrayList<>()).add(tag);
        }
        return map;
    }

    private Map<Long, Long> loadCommentCountMap(List<BlogArticle> articles)
    {
        if (CollectionUtils.isEmpty(articles))
        {
            return Map.of();
        }
        List<Long> articleIds = articles.stream().map(BlogArticle::getId).filter(Objects::nonNull).distinct().toList();
        if (articleIds.isEmpty())
        {
            return Map.of();
        }
        QueryWrapper<BlogComment> wrapper = new QueryWrapper<>();
        wrapper.select("article_id", "COUNT(*) AS cnt");
        wrapper.in("article_id", articleIds);
        wrapper.groupBy("article_id");
        Map<Long, Long> map = new HashMap<>();
        for (Map<String, Object> row : blogCommentMapper.selectMaps(wrapper))
        {
            Object articleId = row.get("article_id");
            Object cnt = row.get("cnt");
            if (articleId != null)
            {
                map.put(((Number) articleId).longValue(), cnt == null ? 0L : ((Number) cnt).longValue());
            }
        }
        return map;
    }

    private ArticleVO toVO(BlogArticle article, Map<Long, String> categoryMap, Map<Long, List<BlogTag>> tagsMap,
            Map<Long, Long> commentCountMap)
    {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        if (article.getCategoryId() != null)
        {
            vo.setCategoryName(categoryMap.get(article.getCategoryId()));
        }
        List<BlogTag> tags = tagsMap.getOrDefault(article.getId(), List.of());
        vo.setTagIds(tags.stream().map(BlogTag::getId).toList());
        vo.setTagNames(tags.stream().map(BlogTag::getName).toList());
        vo.setCommentCount(commentCountMap.getOrDefault(article.getId(), 0L));
        return vo;
    }

    private ArticleBriefVO toBriefVO(BlogArticle article, Map<Long, String> categoryMap, Map<Long, List<BlogTag>> tagsMap)
    {
        ArticleBriefVO vo = new ArticleBriefVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setCoverImage(article.getCoverImage());
        vo.setCategoryId(article.getCategoryId());
        vo.setViewCount(article.getViewCount());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        if (article.getCategoryId() != null)
        {
            vo.setCategoryName(categoryMap.get(article.getCategoryId()));
        }
        List<BlogTag> tags = tagsMap.getOrDefault(article.getId(), List.of());
        vo.setTagNames(tags.stream().map(BlogTag::getName).toList());
        return vo;
    }
}
