package com.ruoyi.wechat.service;

import com.ruoyi.wechat.dto.WechatKefuSendRequest;
import com.ruoyi.wechat.vo.WechatKefuSessionVO;

public interface WechatKefuService
{
    WechatKefuSessionVO checkSession(Long accountId, String openId);

    void sendText(WechatKefuSendRequest request);
}
