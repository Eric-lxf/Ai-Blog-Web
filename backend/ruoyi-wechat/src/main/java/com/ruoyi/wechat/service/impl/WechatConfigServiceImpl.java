package com.ruoyi.wechat.service.impl;

import org.springframework.stereotype.Service;

import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.wechat.config.WechatProperties;
import com.ruoyi.wechat.service.WechatConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatConfigServiceImpl implements WechatConfigService
{
    private final ISysConfigService sysConfigService;
    private final WechatProperties wechatProperties;

    @Override
    public boolean isWechatEnabled()
    {
        String configured = sysConfigService.selectConfigByKey("wechat.enabled");
        if (configured == null || configured.isBlank())
        {
            return wechatProperties.isEnabled();
        }
        return Boolean.parseBoolean(configured);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue)
    {
        String value = sysConfigService.selectConfigByKey(key);
        if (value == null || value.isBlank())
        {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public String getString(String key, String defaultValue)
    {
        String value = sysConfigService.selectConfigByKey(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
