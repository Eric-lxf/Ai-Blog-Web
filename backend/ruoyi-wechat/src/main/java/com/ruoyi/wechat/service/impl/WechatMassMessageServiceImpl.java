package com.ruoyi.wechat.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMassRecord;
import com.ruoyi.wechat.dto.WechatMassPreviewRequest;
import com.ruoyi.wechat.dto.WechatMassSendRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatMassRecordMapper;
import com.ruoyi.wechat.service.WechatMassMessageService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatMassRecordVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatMassMessageServiceImpl implements WechatMassMessageService
{
    private final WechatMassRecordMapper wechatMassRecordMapper;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public Page<WechatMassRecordVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatMassRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatMassRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatMassRecord::getAccountId, query.getAccountId());
        }
        wrapper.orderByDesc(WechatMassRecord::getCreateTime);
        Page<WechatMassRecord> result = wechatMassRecordMapper.selectPage(page, wrapper);
        Page<WechatMassRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public Map<String, Object> preview(WechatMassPreviewRequest request)
    {
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/message/mass/preview?access_token=" + token;
        Map<String, Object> payload = buildMessagePayload(request.getMsgType(), request.getContent(), request.getMediaId());
        payload.put("touser", request.getOpenId());
        payload.put("msgtype", request.getMsgType());
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "preview mass message");
        return resp;
    }

    @Override
    @Transactional
    public Long send(WechatMassSendRequest request)
    {
        if (!Boolean.TRUE.equals(request.getIsToAll()) && request.getWechatTagId() == null)
        {
            throw new ServiceException("wechatTagId is required when isToAll is false", HttpStatus.BAD_REQUEST);
        }
        validateMessage(request.getMsgType(), request.getContent(), request.getMediaId());
        WechatMassRecord record = new WechatMassRecord();
        record.setAccountId(request.getAccountId());
        record.setMsgType(request.getMsgType());
        record.setContent(request.getContent());
        record.setMediaId(request.getMediaId());
        record.setIsToAll(Boolean.TRUE.equals(request.getIsToAll()) ? 1 : 0);
        record.setWechatTagId(request.getWechatTagId());
        record.setStatus("pending");
        wechatMassRecordMapper.insert(record);

        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/message/mass/sendall?access_token=" + token;
        Map<String, Object> payload = buildMessagePayload(request.getMsgType(), request.getContent(), request.getMediaId());
        payload.put("msgtype", request.getMsgType());
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("is_to_all", Boolean.TRUE.equals(request.getIsToAll()));
        if (!Boolean.TRUE.equals(request.getIsToAll()))
        {
            filter.put("tag_id", request.getWechatTagId());
        }
        payload.put("filter", filter);
        try
        {
            Map<String, Object> resp = wechatApiClient.postJson(url, payload);
            WechatApiErrors.assertOk(resp, "send mass message");
            record.setStatus("sent");
            record.setMsgId(parseLong(resp.get("msg_id")));
            record.setResponseBody(objectMapper.writeValueAsString(resp));
            wechatMassRecordMapper.updateById(record);
            return record.getId();
        }
        catch (Exception e)
        {
            record.setStatus("failed");
            record.setResponseBody(e.getMessage());
            wechatMassRecordMapper.updateById(record);
            if (e instanceof ServiceException se)
            {
                throw se;
            }
            throw new ServiceException("send mass message failed: " + e.getMessage(), HttpStatus.ERROR);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> syncStatus(Long recordId)
    {
        WechatMassRecord record = wechatMassRecordMapper.selectById(recordId);
        if (record == null)
        {
            throw new ServiceException("mass record not found", HttpStatus.NOT_FOUND);
        }
        if (record.getMsgId() == null)
        {
            throw new ServiceException("mass record has no msg_id", HttpStatus.BAD_REQUEST);
        }
        String token = wechatTokenService.getAccessToken(record.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/message/mass/get?access_token=" + token + "&msg_id=" + record.getMsgId();
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "query mass message status");
        try
        {
            record.setResponseBody(objectMapper.writeValueAsString(resp));
            wechatMassRecordMapper.updateById(record);
        }
        catch (Exception ignored)
        {
            // keep response in memory only
        }
        return resp;
    }

    private Map<String, Object> buildMessagePayload(String msgType, String content, String mediaId)
    {
        Map<String, Object> payload = new LinkedHashMap<>();
        if ("text".equalsIgnoreCase(msgType))
        {
            payload.put("text", Map.of("content", content == null ? "" : content.trim()));
        }
        else if ("mpnews".equalsIgnoreCase(msgType))
        {
            payload.put("mpnews", Map.of("media_id", mediaId));
        }
        else
        {
            throw new ServiceException("unsupported msgType: " + msgType, HttpStatus.BAD_REQUEST);
        }
        return payload;
    }

    private void validateMessage(String msgType, String content, String mediaId)
    {
        if ("text".equalsIgnoreCase(msgType))
        {
            if (!StringUtils.hasText(content))
            {
                throw new ServiceException("content is required for text mass message", HttpStatus.BAD_REQUEST);
            }
        }
        else if ("mpnews".equalsIgnoreCase(msgType))
        {
            if (!StringUtils.hasText(mediaId))
            {
                throw new ServiceException("mediaId is required for mpnews mass message", HttpStatus.BAD_REQUEST);
            }
        }
        else
        {
            throw new ServiceException("unsupported msgType: " + msgType, HttpStatus.BAD_REQUEST);
        }
    }

    private WechatMassRecordVO toVO(WechatMassRecord source)
    {
        WechatMassRecordVO vo = new WechatMassRecordVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private Long parseLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return Long.parseLong(String.valueOf(value));
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
