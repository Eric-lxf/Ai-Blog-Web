package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatAutoReplySaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatAutoReplyVO;

public interface WechatReplyService
{
    Page<WechatAutoReplyVO> page(WechatPageQuery query);

    Long save(WechatAutoReplySaveRequest request);

    String matchReply(Long accountId, String content);
}
