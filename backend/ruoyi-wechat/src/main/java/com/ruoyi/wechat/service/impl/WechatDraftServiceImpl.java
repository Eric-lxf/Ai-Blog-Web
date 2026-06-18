package com.ruoyi.wechat.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.dto.WechatDraftBatchRequest;
import com.ruoyi.wechat.dto.WechatDraftSaveRequest;
import com.ruoyi.wechat.dto.WechatDraftUpdateRequest;
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
        Map<String, Object> article = buildArticle(
                material.getTitle(),
                material.getAuthor(),
                material.getDigest(),
                material.getContent(),
                material.getContentSourceUrl(),
                material.getThumbMediaId());
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

    @Override
    public Map<String, Object> batchGet(WechatDraftBatchRequest request)
    {
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/batchget?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("offset", request.getOffset() == null ? 0 : request.getOffset());
        payload.put("count", request.getCount() == null ? 10 : Math.min(Math.max(request.getCount(), 1), 20));
        if (request.getNoContent() != null)
        {
            payload.put("no_content", request.getNoContent());
        }
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "batchget draft");
        return resp;
    }

    @Override
    public Map<String, Object> get(Long accountId, String mediaId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/get?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("media_id", mediaId));
        WechatApiErrors.assertOk(resp, "get draft");
        return resp;
    }

    @Override
    public Map<String, Object> add(WechatDraftSaveRequest request)
    {
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/add?access_token=" + token;
        Map<String, Object> article = buildArticle(
                request.getTitle(),
                request.getAuthor(),
                request.getDigest(),
                request.getContent(),
                request.getContentSourceUrl(),
                request.getThumbMediaId());
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("articles", new Object[] { article }));
        WechatApiErrors.assertOk(resp, "add draft");
        return resp;
    }

    @Override
    public void update(WechatDraftUpdateRequest request)
    {
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/update?access_token=" + token;
        Map<String, Object> article = buildArticle(
                request.getTitle(),
                request.getAuthor(),
                request.getDigest(),
                request.getContent(),
                request.getContentSourceUrl(),
                request.getThumbMediaId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("media_id", request.getMediaId());
        payload.put("index", request.getIndex() == null ? 0 : request.getIndex());
        payload.put("articles", article);
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "update draft");
    }

    @Override
    public void delete(Long accountId, String mediaId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/delete?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("media_id", mediaId));
        WechatApiErrors.assertOk(resp, "delete draft");
    }

    @Override
    public Map<String, Object> count(Long accountId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/draft/count?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "count draft");
        return resp;
    }

    private Map<String, Object> buildArticle(String title, String author, String digest, String content,
            String contentSourceUrl, String thumbMediaId)
    {
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title", title == null ? "" : title);
        article.put("author", author == null ? "" : author);
        article.put("digest", digest == null ? "" : digest);
        article.put("content", content == null ? "" : content);
        article.put("content_source_url", contentSourceUrl == null ? "" : contentSourceUrl);
        if (StringUtils.hasText(thumbMediaId))
        {
            article.put("thumb_media_id", thumbMediaId);
        }
        return article;
    }
}
