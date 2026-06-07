package com.ruoyi.wechat.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.domain.WechatMessageLog;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatMessageLogMapper;
import com.ruoyi.wechat.service.WechatMessageService;
import com.ruoyi.wechat.vo.WechatMessageLogVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatMessageServiceImpl implements WechatMessageService
{
    private final WechatMessageLogMapper wechatMessageLogMapper;

    @Override
    public Page<WechatMessageLogVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatMessageLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatMessageLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatMessageLog::getAccountId, query.getAccountId());
        }
        wrapper.orderByDesc(WechatMessageLog::getCreateTime);
        Page<WechatMessageLog> result = wechatMessageLogMapper.selectPage(page, wrapper);
        Page<WechatMessageLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void saveInbound(Long accountId, String openId, String messageType, String eventType, String content, String rawXml)
    {
        WechatMessageLog log = new WechatMessageLog();
        log.setAccountId(accountId);
        log.setDirection("in");
        log.setOpenId(openId);
        log.setMessageType(messageType);
        log.setEventType(eventType);
        log.setContent(content);
        log.setRawXml(rawXml);
        wechatMessageLogMapper.insert(log);
    }

    private WechatMessageLogVO toVO(WechatMessageLog source)
    {
        WechatMessageLogVO vo = new WechatMessageLogVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}
