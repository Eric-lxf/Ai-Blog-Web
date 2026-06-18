package com.ruoyi.wechat.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.service.WechatDraftService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
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
        if (!StringUtils.hasText(material.getThumbMediaId()))
        {
            throw new ServiceException("thumb_media_id is required for wechat draft", HttpStatus.BAD_REQUEST);
        }
        String accessToken = wechatTokenService.getAccessToken(material.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/add?access_token=" + accessToken;
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title", material.getTitle() == null ? "" : material.getTitle());
        article.put("author", material.getAuthor() == null ? "" : material.getAuthor());
        article.put("digest", material.getDigest() == null ? "" : material.getDigest());
        article.put("content", material.getContent() == null ? "" : material.getContent());
        article.put("content_source_url", material.getContentSourceUrl() == null ? "" : material.getContentSourceUrl());
        article.put("thumb_media_id", material.getThumbMediaId());
        Map<String, Object> payload = Map.of("articles", new Object[] { article });
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "create wechat draft");
        Object mediaId = resp.get("media_id");
        if (mediaId == null)
        {
            throw new ServiceException("create wechat draft failed: media_id missing", HttpStatus.ERROR);
        }
        material.setMediaId(String.valueOf(mediaId));
        material.setStatus(1);
        return material;
    }
}
