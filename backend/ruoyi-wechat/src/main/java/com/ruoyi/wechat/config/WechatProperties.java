package com.ruoyi.wechat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties
{
    private boolean enabled = true;
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 30000;
}
