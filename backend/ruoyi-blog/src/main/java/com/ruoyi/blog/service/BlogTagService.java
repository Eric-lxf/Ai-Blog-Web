package com.ruoyi.blog.service;

import java.util.List;

import com.ruoyi.blog.domain.BlogTag;

public interface BlogTagService
{

    List<BlogTag> listAll();

    List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames);
}
