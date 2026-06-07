package com.ruoyi.wechat.service;

public interface WechatConfigService
{
    boolean isWechatEnabled();

    boolean getBoolean(String key, boolean defaultValue);

    String getString(String key, String defaultValue);
}
