package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.ArticlePageQuery;
import com.ruoyi.blog.constant.BlogAnalyticsConstants;
import com.ruoyi.blog.service.BlogArticleService;
import com.ruoyi.blog.service.BlogVisitService;
import com.ruoyi.blog.vo.ArticleBriefVO;
import com.ruoyi.blog.vo.ArticleVO;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

import lombok.RequiredArgsConstructor;

@Anonymous
@RestController
@RequestMapping("/public/blog")
@RequiredArgsConstructor
public class PublicArticleController extends BlogControllerSupport
{

    private final BlogArticleService blogArticleService;
    private final BlogVisitService blogVisitService;

    @GetMapping("/articles")
    public TableDataInfo page(@Valid ArticlePageQuery query)
    {
        blogVisitService.recordFromRequest(BlogAnalyticsConstants.PAGE_LIST, null);
        Page<ArticleBriefVO> page = blogArticleService.publicPage(query);
        return mpPageTable(page);
    }

    @GetMapping("/articles/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(blogArticleService.getPublishedById(id));
    }
}
