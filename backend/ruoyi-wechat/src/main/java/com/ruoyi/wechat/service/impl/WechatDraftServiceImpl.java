package com.ruoyi.wechat.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.service.WechatDraftService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatDraftServiceImpl implements WechatDraftService
{
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;

    @Override
    public WechatMaterial createDraft(WechatMaterial material)
    {
        String accessToken = wechatTokenService.getAccessToken(material.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/add?access_token=" + accessToken;
        Map<String, Object> payload = Map.of("articles", new Object[] { Map.of(
                "title", material.getTitle() == null ? "" : material.getTitle(),
                "author", material.getAuthor() == null ? "" : material.getAuthor(),
                "digest", material.getDigest() == null ? "" : material.getDigest(),
                "content", material.getContent() == null ? "" : material.getContent(),
                "content_source_url", material.getContentSourceUrl() == null ? "" : material.getContentSourceUrl()) });
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        Object mediaId = resp.get("media_id");
        if (mediaId != null)
        {
            material.setMediaId(String.valueOf(mediaId));
            material.setStatus(1);
        }
        return material;
    }
}
