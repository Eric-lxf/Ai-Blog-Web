package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatFansSyncResultVO;
import com.ruoyi.wechat.vo.WechatFansVO;

public interface WechatFansService
{
    Page<WechatFansVO> page(WechatPageQuery query);

    WechatFansSyncResultVO syncFromWechat(Long accountId);

    void handleSubscribeEvent(Long accountId, String openId, boolean subscribed);
}
