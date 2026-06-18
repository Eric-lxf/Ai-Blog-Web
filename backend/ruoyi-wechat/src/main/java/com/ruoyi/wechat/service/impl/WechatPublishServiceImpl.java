package com.ruoyi.wechat.service.impl;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.domain.WechatPublishRecord;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatMaterialMapper;
import com.ruoyi.wechat.mapper.WechatPublishRecordMapper;
import com.ruoyi.wechat.service.WechatFreePublishService;
import com.ruoyi.wechat.service.WechatPublishService;
import com.ruoyi.wechat.vo.WechatPublishRecordVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatPublishServiceImpl implements WechatPublishService
{
    private final WechatPublishRecordMapper wechatPublishRecordMapper;
    private final WechatMaterialMapper wechatMaterialMapper;
    private final WechatFreePublishService wechatFreePublishService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<WechatPublishRecordVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatPublishRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatPublishRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatPublishRecord::getAccountId, query.getAccountId());
        }
        if (query.getStatus() != null)
        {
            wrapper.eq(WechatPublishRecord::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(WechatPublishRecord::getUpdateTime);
        Page<WechatPublishRecord> result = wechatPublishRecordMapper.selectPage(page, wrapper);
        Page<WechatPublishRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public Long save(WechatPublishRecord record)
    {
        if (record.getStatus() == null)
        {
            record.setStatus(WechatConstants.PUBLISH_STATUS_PENDING);
        }
        if (record.getId() == null)
        {
            wechatPublishRecordMapper.insert(record);
        }
        else
        {
            if (wechatPublishRecordMapper.updateById(record) == 0)
            {
                throw new ServiceException("publish record not found", HttpStatus.NOT_FOUND);
            }
        }
        return record.getId();
    }

    @Override
    @Transactional
    public void markSuccess(Long id, String msgId, String responseBody)
    {
        WechatPublishRecord record = requireRecord(id);
        record.setStatus(WechatConstants.PUBLISH_STATUS_PUBLISHED);
        record.setMsgId(msgId);
        record.setResponseBody(responseBody);
        wechatPublishRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void markFailed(Long id, String errorMessage, String responseBody)
    {
        WechatPublishRecord record = requireRecord(id);
        record.setStatus(WechatConstants.PUBLISH_STATUS_FAILED);
        record.setErrorMessage(errorMessage);
        record.setResponseBody(responseBody);
        wechatPublishRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public Map<String, Object> submitFromRecord(Long recordId)
    {
        WechatPublishRecord record = requireRecord(recordId);
        WechatMaterial material = requireMaterial(record.getMaterialId());
        if (!StringUtils.hasText(material.getMediaId()))
        {
            throw new ServiceException("draft media_id is missing, create draft first", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> resp = wechatFreePublishService.submit(record.getAccountId(), material.getMediaId());
        String publishId = String.valueOf(resp.getOrDefault("publish_id", ""));
        record.setStatus(WechatConstants.PUBLISH_STATUS_PUBLISHING);
        record.setMsgId(publishId);
        record.setResponseBody(toJson(resp));
        record.setErrorMessage(null);
        wechatPublishRecordMapper.updateById(record);
        return resp;
    }

    @Override
    @Transactional
    public Map<String, Object> syncStatus(Long recordId)
    {
        WechatPublishRecord record = requireRecord(recordId);
        if (!StringUtils.hasText(record.getMsgId()))
        {
            throw new ServiceException("publish_id is missing on record", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> resp = wechatFreePublishService.getStatus(record.getAccountId(), record.getMsgId());
        applyPublishStatus(record, resp);
        wechatPublishRecordMapper.updateById(record);
        return resp;
    }

    private void applyPublishStatus(WechatPublishRecord record, Map<String, Object> resp)
    {
        Object statusObj = resp.get("publish_status");
        int publishStatus = statusObj == null ? -1 : Integer.parseInt(String.valueOf(statusObj));
        record.setResponseBody(toJson(resp));
        if (publishStatus == 0)
        {
            record.setStatus(WechatConstants.PUBLISH_STATUS_PUBLISHED);
            Object articleId = resp.get("article_id");
            if (articleId != null)
            {
                record.setMsgId(String.valueOf(articleId));
            }
            record.setErrorMessage(null);
            return;
        }
        if (publishStatus == 1)
        {
            record.setStatus(WechatConstants.PUBLISH_STATUS_PUBLISHING);
            record.setErrorMessage(null);
            return;
        }
        if (publishStatus >= 2)
        {
            record.setStatus(WechatConstants.PUBLISH_STATUS_FAILED);
            record.setErrorMessage("publish_status=" + publishStatus);
        }
    }

    private WechatPublishRecord requireRecord(Long id)
    {
        WechatPublishRecord record = wechatPublishRecordMapper.selectById(id);
        if (record == null)
        {
            throw new ServiceException("publish record not found", HttpStatus.NOT_FOUND);
        }
        return record;
    }

    private WechatMaterial requireMaterial(Long materialId)
    {
        if (materialId == null)
        {
            throw new ServiceException("material id is missing on record", HttpStatus.BAD_REQUEST);
        }
        WechatMaterial material = wechatMaterialMapper.selectById(materialId);
        if (material == null)
        {
            throw new ServiceException("material not found", HttpStatus.NOT_FOUND);
        }
        return material;
    }

    private WechatPublishRecordVO toVO(WechatPublishRecord record)
    {
        WechatPublishRecordVO vo = new WechatPublishRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
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
