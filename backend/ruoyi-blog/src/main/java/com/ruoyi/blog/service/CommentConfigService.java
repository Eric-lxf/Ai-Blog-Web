package com.ruoyi.blog.service;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentConfigService
{
    private final ISysConfigService sysConfigService;

    public boolean requireAudit()
    {
        return getBool("blog.comment.requireAudit", true);
    }

    public boolean anonymousEnabled()
    {
        return getBool("blog.comment.anonymous.enabled", true);
    }

    public int maxLength()
    {
        return getInt("blog.comment.maxLength", 2000);
    }

    public int rateLimitPerMinute()
    {
        return getInt("blog.comment.rateLimitPerMinute", 5);
    }

    public boolean aiEnabled()
    {
        return getBool("blog.comment.ai.enabled", true);
    }

    public int aiAutoRejectScore()
    {
        return getInt("blog.comment.ai.autoRejectScore", 80);
    }

    public int aiAutoPassScore()
    {
        return getInt("blog.comment.ai.autoPassScore", 20);
    }

    private boolean getBool(String key, boolean defaultValue)
    {
        String value = sysConfigService.selectConfigByKey(key);
        if (StringUtils.isEmpty(value))
        {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private int getInt(String key, int defaultValue)
    {
        String value = sysConfigService.selectConfigByKey(key);
        if (StringUtils.isEmpty(value))
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex)
        {
            return defaultValue;
        }
    }
}
