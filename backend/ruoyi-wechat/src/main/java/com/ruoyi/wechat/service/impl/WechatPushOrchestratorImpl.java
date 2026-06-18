package com.ruoyi.wechat.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.blog.domain.BlogArticle;
import com.ruoyi.blog.mapper.BlogArticleMapper;
import com.ruoyi.blog.service.BlogArticleService;
import com.ruoyi.blog.vo.ArticleVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.domain.WechatPublishRecord;
import com.ruoyi.wechat.dto.WechatPushRequest;
import com.ruoyi.wechat.service.WechatDraftService;
import com.ruoyi.wechat.service.WechatPublishService;
import com.ruoyi.wechat.service.WechatPushOrchestrator;
import com.ruoyi.wechat.service.WechatMaterialService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatContentConverter;
import com.ruoyi.wechat.support.WechatMediaUploadService;
import com.ruoyi.wechat.support.WechatTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatPushOrchestratorImpl implements WechatPushOrchestrator
{
    private final BlogArticleService blogArticleService;
    private final BlogArticleMapper blogArticleMapper;
    private final WechatContentConverter wechatContentConverter;
    private final WechatMaterialService wechatMaterialService;
    private final WechatDraftService wechatDraftService;
    private final WechatPublishService wechatPublishService;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;
    private final WechatMediaUploadService wechatMediaUploadService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long push(WechatPushRequest request)
    {
        ArticleVO article = blogArticleService.getById(request.getArticleId());
        if (article.getStatus() == null || article.getStatus() != 1)
        {
            throw new ServiceException("only published articles can be pushed", HttpStatus.ERROR);
        }
        if (!StringUtils.hasText(article.getCoverImage()))
        {
            throw new ServiceException("article cover image is required for wechat push", HttpStatus.BAD_REQUEST);
        }

        BlogArticle articleRow = blogArticleMapper.selectById(request.getArticleId());
        String htmlContent = articleRow == null ? null : articleRow.getHtmlContent();
        String rawContent = StringUtils.hasText(htmlContent) ? htmlContent : article.getContent();

        String thumbMediaId = wechatMediaUploadService.uploadThumb(request.getAccountId(), article.getCoverImage());

        WechatMaterial material = new WechatMaterial();
        material.setAccountId(request.getAccountId());
        material.setTitle(article.getTitle());
        material.setAuthor("RuoYi");
        material.setDigest(wechatContentConverter.toDigest(rawContent));
        material.setContent(wechatContentConverter.toWechatHtml(rawContent, request.getAccountId()));
        material.setContentSourceUrl("");
        material.setThumbMediaId(thumbMediaId);
        material.setStatus(0);
        wechatMaterialService.save(material);

        try
        {
            material = wechatDraftService.createDraft(material);
        }
        catch (ServiceException e)
        {
            wechatMaterialService.save(material);
            throw e;
        }
        wechatMaterialService.save(material);

        WechatPublishRecord record = new WechatPublishRecord();
        record.setAccountId(request.getAccountId());
        record.setArticleId(request.getArticleId());
        record.setMaterialId(material.getId());
        record.setPublishMode(request.getPublishMode());
        record.setStatus(WechatConstants.PUBLISH_STATUS_DRAFT_OK);
        wechatPublishService.save(record);

        if (!WechatConstants.PUSH_MODE_DRAFT_AND_PUBLISH.equalsIgnoreCase(request.getPublishMode()))
        {
            return record.getId();
        }

        String accessToken = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/freepublish/submit?access_token=" + accessToken;
        Map<String, Object> payload = Map.of("media_id", material.getMediaId());
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);

        try
        {
            WechatApiErrors.assertOk(resp, "wechat publish");
        }
        catch (ServiceException e)
        {
            wechatPublishService.markFailed(record.getId(), "publish failed", toJson(resp));
            throw e;
        }
        String publishId = String.valueOf(resp.getOrDefault("publish_id", ""));
        wechatPublishService.markSuccess(record.getId(), publishId, toJson(resp));
        return record.getId();
    }

    private String toJson(Object data)
    {
        try
        {
            return objectMapper.writeValueAsString(data);
        }
        catch (JsonProcessingException e)
        {
            return String.valueOf(data);
        }
    }
}
