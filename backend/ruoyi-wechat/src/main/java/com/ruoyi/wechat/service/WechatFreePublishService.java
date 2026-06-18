package com.ruoyi.wechat.service;

import java.util.Map;

public interface WechatFreePublishService
{
    Map<String, Object> batchGet(Long accountId, int offset, int count, Integer noContent);

    void delete(Long accountId, String articleId, Integer index);

    Map<String, Object> getStatus(Long accountId, String publishId);

    Map<String, Object> getArticle(Long accountId, String articleId);

    Map<String, Object> submit(Long accountId, String mediaId);
}
