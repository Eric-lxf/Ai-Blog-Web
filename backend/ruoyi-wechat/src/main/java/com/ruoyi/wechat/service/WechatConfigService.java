package com.ruoyi.wechat.service;

import com.ruoyi.wechat.dto.WechatModuleConfigUpdateRequest;
import com.ruoyi.wechat.vo.WechatModuleConfigVO;

public interface WechatConfigService
{
    boolean isWechatEnabled();

    WechatModuleConfigVO getModuleConfig();

    void updateModuleConfig(WechatModuleConfigUpdateRequest request);

    boolean getBoolean(String key, boolean defaultValue);

    String getString(String key, String defaultValue);
}
