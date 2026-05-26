package com.ruoyi.blog.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.domain.BlogTag;
import com.ruoyi.blog.mapper.BlogTagMapper;
import com.ruoyi.blog.service.BlogTagService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogTagServiceImpl implements BlogTagService
{

    private final BlogTagMapper blogTagMapper;

    @Override
    public List<BlogTag> listAll()
    {
        return blogTagMapper.selectList(new LambdaQueryWrapper<BlogTag>().orderByDesc(BlogTag::getCreateTime));
    }

    @Override
    public List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames)
    {
        Set<Long> result = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(tagIds))
        {
            result.addAll(tagIds);
        }
        if (CollectionUtils.isEmpty(tagNames))
        {
            return new ArrayList<>(result);
        }
        for (String name : tagNames)
        {
            if (!StringUtils.hasText(name))
            {
                continue;
            }
            String trimmed = name.trim();
            BlogTag existing = blogTagMapper.selectOne(new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getName, trimmed).last("LIMIT 1"));
            if (existing != null)
            {
                result.add(existing.getId());
            }
            else
            {
                BlogTag tag = new BlogTag();
                tag.setName(trimmed);
                blogTagMapper.insert(tag);
                result.add(tag.getId());
            }
        }
        return new ArrayList<>(result);
    }
}
