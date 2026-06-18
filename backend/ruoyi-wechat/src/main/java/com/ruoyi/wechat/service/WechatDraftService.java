package com.ruoyi.wechat.service;

import java.util.Map;

import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.dto.WechatDraftBatchRequest;
import com.ruoyi.wechat.dto.WechatDraftSaveRequest;
import com.ruoyi.wechat.dto.WechatDraftUpdateRequest;

public interface WechatDraftService
{
    WechatMaterial createDraft(WechatMaterial material);

    Map<String, Object> batchGet(WechatDraftBatchRequest request);

    Map<String, Object> get(Long accountId, String mediaId);

    Map<String, Object> add(WechatDraftSaveRequest request);

    void update(WechatDraftUpdateRequest request);

    void delete(Long accountId, String mediaId);

    Map<String, Object> count(Long accountId);
}
