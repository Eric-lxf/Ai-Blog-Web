package com.ruoyi.blog.service;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationConfigService
{
    private final ISysConfigService sysConfigService;

    public boolean enabled()
    {
        return getBool("blog.notification.enabled", true);
    }

    public boolean emailEnabled()
    {
        return getBool("blog.notification.email.enabled", false);
    }

    public String publicBaseUrl()
    {
        String value = sysConfigService.selectConfigByKey("blog.notification.publicBaseUrl");
        return StringUtils.isEmpty(value) ? "http://localhost" : value.trim();
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
}
