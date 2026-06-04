package com.ruoyi.blog.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ruoyi.blog.domain.BlogArticle;
import com.ruoyi.blog.dto.AiWriteWizardRequest;
import com.ruoyi.blog.mapper.BlogArticleMapper;
import com.ruoyi.blog.mapper.BlogArticleTagMapper;
import com.ruoyi.blog.service.AiWriteArticlePersistence;
import com.ruoyi.blog.service.BlogTagService;
import com.ruoyi.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiWriteArticlePersistenceImpl implements AiWriteArticlePersistence
{

    private final BlogArticleMapper blogArticleMapper;

    private final BlogArticleTagMapper blogArticleTagMapper;

    private final BlogTagService blogTagService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveGeneratedDraft(AiWriteWizardRequest request, String content)
    {
        BlogArticle draft = new BlogArticle();
        draft.setTitle(request.getTitle());
        draft.setSummary(request.getSummary());
        draft.setContent(content);
        draft.setCategoryId(request.getCategoryId());
        draft.setStatus(Boolean.TRUE.equals(request.getPublish()) ? 1 : 0);
        draft.setIsAiGenerated(1);
        draft.setViewCount(0);
        try
        {
            draft.setAuthorUserId(SecurityUtils.getUserId());
        }
        catch (Exception ignored)
        {
            // 无登录上下文
        }
        blogArticleMapper.insert(draft);

        if (!CollectionUtils.isEmpty(request.getTagNames()))
        {
            List<Long> tagIds = blogTagService.resolveTagIds(Collections.emptyList(), request.getTagNames());
            if (!tagIds.isEmpty())
            {
                blogArticleTagMapper.batchInsert(draft.getId(), tagIds);
            }
        }
        return draft.getId();
    }
}
