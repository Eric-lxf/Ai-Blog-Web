package com.ruoyi.wechat.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.domain.WechatFans;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatFansMapper;
import com.ruoyi.wechat.service.WechatFansService;
import com.ruoyi.wechat.vo.WechatFansVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatFansServiceImpl implements WechatFansService
{
    private final WechatFansMapper wechatFansMapper;

    @Override
    public Page<WechatFansVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatFans> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatFans> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatFans::getAccountId, query.getAccountId());
        }
        wrapper.orderByDesc(WechatFans::getUpdateTime);
        Page<WechatFans> result = wechatFansMapper.selectPage(page, wrapper);
        Page<WechatFansVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    private WechatFansVO toVO(WechatFans source)
    {
        WechatFansVO vo = new WechatFansVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}
