package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.ArticlePageQuery;
import com.ruoyi.blog.service.BlogArticleService;
import com.ruoyi.blog.vo.ArticleVO;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/article/recycle")
@RequiredArgsConstructor
public class BlogArticleRecycleController extends BlogControllerSupport
{

    private final BlogArticleService blogArticleService;

    @PreAuthorize("@ss.hasPermi('blog:article:recycle')")
    @GetMapping
    public TableDataInfo page(@Valid ArticlePageQuery query)
    {
        Page<ArticleVO> page = blogArticleService.recyclePage(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:article:restore')")
    @PutMapping("/{id}/restore")
    public AjaxResult restore(@PathVariable Long id)
    {
        blogArticleService.restore(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('blog:article:purge')")
    @DeleteMapping("/{id}")
    public AjaxResult purge(@PathVariable Long id)
    {
        blogArticleService.purge(id);
        return AjaxResult.success();
    }
}
