package com.ruoyi.blog.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.domain.BlogTag;
import com.ruoyi.blog.service.BlogTagService;
import com.ruoyi.common.core.domain.AjaxResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/tag")
@RequiredArgsConstructor
public class BlogTagController extends BlogControllerSupport
{

    private final BlogTagService blogTagService;

    @PreAuthorize("@ss.hasPermi('blog:article:list')")
    @GetMapping
    public AjaxResult list()
    {
        List<BlogTag> list = blogTagService.listAll();
        return AjaxResult.success(list);
    }
}
