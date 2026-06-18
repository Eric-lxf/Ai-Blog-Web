package com.ruoyi.wechat.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.domain.WechatAutoReply;
import com.ruoyi.wechat.dto.WechatAutoReplySaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatAutoReplyMapper;
import com.ruoyi.wechat.service.WechatReplyService;
import com.ruoyi.wechat.support.WechatReplyMatcher;
import com.ruoyi.wechat.vo.WechatAutoReplyVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatReplyServiceImpl implements WechatReplyService
{
    private final WechatAutoReplyMapper wechatAutoReplyMapper;

    @Override
    public Page<WechatAutoReplyVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatAutoReply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatAutoReply> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatAutoReply::getAccountId, query.getAccountId());
        }
        wrapper.orderByDesc(WechatAutoReply::getUpdateTime);
        Page<WechatAutoReply> result = wechatAutoReplyMapper.selectPage(page, wrapper);
        Page<WechatAutoReplyVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public Long save(WechatAutoReplySaveRequest request)
    {
        WechatAutoReply entity = new WechatAutoReply();
        BeanUtils.copyProperties(request, entity);
        if (entity.getId() == null)
        {
            wechatAutoReplyMapper.insert(entity);
        }
        else
        {
            wechatAutoReplyMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public String resolveReply(Long accountId, String msgType, String event, String eventKey, String content)
    {
        if (accountId == null)
        {
            return "";
        }
        LambdaQueryWrapper<WechatAutoReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WechatAutoReply::getAccountId, accountId).eq(WechatAutoReply::getEnabled, 1);
        return WechatReplyMatcher.resolve(wechatAutoReplyMapper.selectList(wrapper), msgType, event, eventKey, content);
    }

    private WechatAutoReplyVO toVO(WechatAutoReply source)
    {
        WechatAutoReplyVO vo = new WechatAutoReplyVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}
