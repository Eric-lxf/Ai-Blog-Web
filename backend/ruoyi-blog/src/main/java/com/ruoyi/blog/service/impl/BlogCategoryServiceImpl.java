package com.ruoyi.blog.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.domain.BlogCategory;
import com.ruoyi.blog.mapper.BlogCategoryMapper;
import com.ruoyi.blog.service.BlogCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl implements BlogCategoryService
{

    private final BlogCategoryMapper blogCategoryMapper;

    @Override
    public List<BlogCategory> listAll()
    {
        return blogCategoryMapper.selectList(new LambdaQueryWrapper<BlogCategory>().orderByAsc(BlogCategory::getSortOrder));
    }
}
