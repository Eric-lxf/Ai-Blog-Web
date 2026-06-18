package com.ruoyi.wechat.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.service.WechatFreePublishService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatFreePublishServiceImpl implements WechatFreePublishService
{
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;

    @Override
    public Map<String, Object> batchGet(Long accountId, int offset, int count, Integer noContent)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/freepublish/batchget?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("offset", Math.max(offset, 0));
        payload.put("count", Math.min(Math.max(count, 1), 20));
        if (noContent != null)
        {
            payload.put("no_content", noContent);
        }
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "batchget published messages");
        return resp;
    }

    @Override
    public void delete(Long accountId, String articleId, Integer index)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/freepublish/delete?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("article_id", articleId);
        if (index != null)
        {
            payload.put("index", index);
        }
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "delete published article");
    }

    @Override
    public Map<String, Object> getStatus(Long accountId, String publishId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/freepublish/get?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("publish_id", publishId));
        WechatApiErrors.assertOk(resp, "query publish status");
        return resp;
    }

    @Override
    public Map<String, Object> getArticle(Long accountId, String articleId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/freepublish/getarticle?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("article_id", articleId));
        WechatApiErrors.assertOk(resp, "get published article");
        return resp;
    }

    @Override
    public Map<String, Object> submit(Long accountId, String mediaId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/freepublish/submit?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("media_id", mediaId));
        WechatApiErrors.assertOk(resp, "submit publish");
        return resp;
    }
}
