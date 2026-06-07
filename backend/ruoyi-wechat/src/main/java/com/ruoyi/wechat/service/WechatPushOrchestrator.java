package com.ruoyi.wechat.service;

import com.ruoyi.wechat.dto.WechatPushRequest;

public interface WechatPushOrchestrator
{
    Long push(WechatPushRequest request);
}
