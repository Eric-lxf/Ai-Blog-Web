package com.ruoyi.blog.service;

import java.util.List;

import com.ruoyi.blog.domain.BlogSensitiveWord;

public interface BlogSensitiveWordService
{
    List<BlogSensitiveWord> list(BlogSensitiveWord query);

    BlogSensitiveWord getById(Long id);

    int insert(BlogSensitiveWord word);

    int update(BlogSensitiveWord word);

    int deleteByIds(Long[] ids);
}
