package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatMessageLogVO;

public interface WechatMessageService
{
    Page<WechatMessageLogVO> page(WechatPageQuery query);

    void saveInbound(Long accountId, String openId, String messageType, String eventType, String content, String rawXml);
}
