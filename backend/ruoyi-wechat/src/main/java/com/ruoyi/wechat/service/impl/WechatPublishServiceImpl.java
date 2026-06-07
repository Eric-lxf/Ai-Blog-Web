package com.ruoyi.wechat.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatPublishRecord;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatPublishRecordMapper;
import com.ruoyi.wechat.service.WechatPublishService;
import com.ruoyi.wechat.vo.WechatPublishRecordVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatPublishServiceImpl implements WechatPublishService
{
    private final WechatPublishRecordMapper wechatPublishRecordMapper;

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

    private WechatPublishRecord requireRecord(Long id)
    {
        WechatPublishRecord record = wechatPublishRecordMapper.selectById(id);
        if (record == null)
        {
            throw new ServiceException("publish record not found", HttpStatus.NOT_FOUND);
        }
        return record;
    }

    private WechatPublishRecordVO toVO(WechatPublishRecord record)
    {
        WechatPublishRecordVO vo = new WechatPublishRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
