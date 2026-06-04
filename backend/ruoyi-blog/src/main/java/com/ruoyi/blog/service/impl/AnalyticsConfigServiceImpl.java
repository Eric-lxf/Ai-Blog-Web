package com.ruoyi.blog.service.impl;

import org.springframework.stereotype.Service;

import com.ruoyi.blog.service.AnalyticsConfigService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsConfigServiceImpl implements AnalyticsConfigService
{
    private static final String KEY_ENABLED = "blog.analytics.enabled";

    private final ISysConfigService sysConfigService;

    @Override
    public boolean enabled()
    {
        String value = sysConfigService.selectConfigByKey(KEY_ENABLED);
        return !"false".equalsIgnoreCase(StringUtils.trim(value));
    }
}
