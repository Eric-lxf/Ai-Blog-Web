package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.ArticlePageQuery;
import com.ruoyi.blog.dto.ArticleSaveRequest;
import com.ruoyi.blog.service.BlogArticleService;
import com.ruoyi.blog.vo.ArticleVO;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/article")
@RequiredArgsConstructor
public class BlogArticleController extends BlogControllerSupport
{

    private final BlogArticleService blogArticleService;

    @PreAuthorize("@ss.hasPermi('blog:article:list')")
    @GetMapping
    public TableDataInfo page(@Valid ArticlePageQuery query)
    {
        Page<ArticleVO> page = blogArticleService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:article:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(blogArticleService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('blog:article:add') or @ss.hasPermi('blog:article:edit')")
    @PostMapping
    public AjaxResult save(@Valid @RequestBody ArticleSaveRequest request)
    {
        Long id = blogArticleService.save(request);
        return AjaxResult.success(id);
    }

    @PreAuthorize("@ss.hasPermi('blog:article:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        blogArticleService.delete(id);
        return AjaxResult.success();
    }
}
