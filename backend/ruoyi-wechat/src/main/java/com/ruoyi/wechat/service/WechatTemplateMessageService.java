package com.ruoyi.wechat.service;

import java.util.List;

import com.ruoyi.wechat.dto.WechatTemplateSendRequest;
import com.ruoyi.wechat.vo.WechatPrivateTemplateVO;

public interface WechatTemplateMessageService
{
    List<WechatPrivateTemplateVO> listTemplates(Long accountId);

    void send(WechatTemplateSendRequest request);
}
