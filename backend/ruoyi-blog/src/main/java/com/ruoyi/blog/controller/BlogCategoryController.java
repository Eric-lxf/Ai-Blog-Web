package com.ruoyi.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.domain.BlogCategory;
import com.ruoyi.blog.service.BlogCategoryService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;

import lombok.RequiredArgsConstructor;

@Anonymous
@RestController
@RequestMapping("/blog/category")
@RequiredArgsConstructor
public class BlogCategoryController extends BlogControllerSupport
{

    private final BlogCategoryService blogCategoryService;

    @GetMapping
    public AjaxResult list()
    {
        List<BlogCategory> list = blogCategoryService.listAll();
        return AjaxResult.success(list);
    }
}
