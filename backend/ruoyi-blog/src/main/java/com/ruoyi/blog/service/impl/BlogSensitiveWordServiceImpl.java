package com.ruoyi.blog.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.domain.BlogSensitiveWord;
import com.ruoyi.blog.mapper.BlogSensitiveWordMapper;
import com.ruoyi.blog.service.BlogSensitiveWordService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogSensitiveWordServiceImpl implements BlogSensitiveWordService
{
    private final BlogSensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    public List<BlogSensitiveWord> list(BlogSensitiveWord query)
    {
        LambdaQueryWrapper<BlogSensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (query != null && StringUtils.hasText(query.getWord()))
        {
            wrapper.like(BlogSensitiveWord::getWord, query.getWord().trim());
        }
        if (query != null && query.getStatus() != null)
        {
            wrapper.eq(BlogSensitiveWord::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BlogSensitiveWord::getUpdateTime);
        return sensitiveWordMapper.selectList(wrapper);
    }

    @Override
    public BlogSensitiveWord getById(Long id)
    {
        return sensitiveWordMapper.selectById(id);
    }

    @Override
    public int insert(BlogSensitiveWord word)
    {
        int rows = sensitiveWordMapper.insert(word);
        sensitiveWordFilter.reload();
        return rows;
    }

    @Override
    public int update(BlogSensitiveWord word)
    {
        int rows = sensitiveWordMapper.updateById(word);
        sensitiveWordFilter.reload();
        return rows;
    }

    @Override
    public int deleteByIds(Long[] ids)
    {
        int rows = sensitiveWordMapper.deleteByIds(Arrays.asList(ids));
        sensitiveWordFilter.reload();
        return rows;
    }
}
