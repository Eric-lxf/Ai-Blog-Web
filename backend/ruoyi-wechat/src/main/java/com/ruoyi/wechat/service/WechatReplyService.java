package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatAutoReplySaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatAutoReplyVO;

public interface WechatReplyService
{
    Page<WechatAutoReplyVO> page(WechatPageQuery query);

    Long save(WechatAutoReplySaveRequest request);

    /**
     * Resolve passive reply content from local rules (not synced to WeChat MP autoreply UI).
     *
     * @param msgType  message type from callback xml, e.g. text / event
     * @param event    event name when msgType is event, e.g. subscribe / SCAN
     * @param eventKey event key when msgType is event, e.g. qrscene_123
     * @param content  user text when msgType is text
     */
    String resolveReply(Long accountId, String msgType, String event, String eventKey, String content);
}
