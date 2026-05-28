package com.ruoyi.blog.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.constant.BlogCommentConstants;
import com.ruoyi.blog.domain.BlogSensitiveWord;
import com.ruoyi.blog.mapper.BlogSensitiveWordMapper;
import com.ruoyi.common.exception.ServiceException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SensitiveWordFilter
{
    private final BlogSensitiveWordMapper sensitiveWordMapper;

    private volatile List<BlogSensitiveWord> words = List.of();

    @PostConstruct
    public void init()
    {
        reload();
    }

    public void reload()
    {
        words = sensitiveWordMapper.selectList(new LambdaQueryWrapper<BlogSensitiveWord>()
                .eq(BlogSensitiveWord::getStatus, 1));
    }

    public FilterResult filter(String content)
    {
        if (content == null || content.isBlank())
        {
            return FilterResult.pass(content);
        }
        String result = content;
        boolean forceReview = false;
        for (BlogSensitiveWord word : words)
        {
            if (!matches(result, word))
            {
                continue;
            }
            String action = word.getAction() == null ? BlogCommentConstants.ACTION_BLOCK : word.getAction();
            switch (action)
            {
                case BlogCommentConstants.ACTION_BLOCK ->
                    throw new ServiceException("评论包含敏感词，请修改后重试");
                case BlogCommentConstants.ACTION_REPLACE ->
                    result = replace(result, word);
                case BlogCommentConstants.ACTION_REVIEW ->
                    forceReview = true;
                default ->
                {
                }
            }
        }
        return new FilterResult(result, forceReview);
    }

    private boolean matches(String content, BlogSensitiveWord word)
    {
        String w = word.getWord();
        if (w == null || w.isBlank())
        {
            return false;
        }
        String mode = word.getMatchMode() == null ? "contains" : word.getMatchMode();
        if ("exact".equalsIgnoreCase(mode))
        {
            return content.toLowerCase(Locale.ROOT).equals(w.toLowerCase(Locale.ROOT));
        }
        return content.toLowerCase(Locale.ROOT).contains(w.toLowerCase(Locale.ROOT));
    }

    private String replace(String content, BlogSensitiveWord word)
    {
        String replacement = word.getReplaceText() == null ? "***" : word.getReplaceText();
        if ("exact".equalsIgnoreCase(word.getMatchMode()))
        {
            return content.replace(word.getWord(), replacement);
        }
        return content.replaceAll("(?i)" + java.util.regex.Pattern.quote(word.getWord()), replacement);
    }

    public record FilterResult(String content, boolean forceReview)
    {
        static FilterResult pass(String content)
        {
            return new FilterResult(content, false);
        }
    }
}
