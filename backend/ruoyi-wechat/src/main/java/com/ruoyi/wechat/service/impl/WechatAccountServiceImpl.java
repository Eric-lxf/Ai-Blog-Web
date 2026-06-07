package com.ruoyi.wechat.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.domain.WechatAccount;
import com.ruoyi.wechat.dto.WechatAccountSaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatAccountMapper;
import com.ruoyi.wechat.service.WechatAccountService;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatAccountVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatAccountServiceImpl implements WechatAccountService
{
    private final WechatAccountMapper wechatAccountMapper;
    private final WechatTokenService wechatTokenService;

    @Override
    public Page<WechatAccountVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatAccount> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.like(WechatAccount::getName, query.getKeyword());
        }
        if (query.getStatus() != null)
        {
            wrapper.eq(WechatAccount::getEnabled, query.getStatus());
        }
        wrapper.orderByDesc(WechatAccount::getUpdateTime);
        Page<WechatAccount> result = wechatAccountMapper.selectPage(page, wrapper);
        Page<WechatAccountVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public WechatAccountVO getById(Long id)
    {
        WechatAccount account = wechatAccountMapper.selectById(id);
        if (account == null)
        {
            throw new ServiceException("wechat account not found", HttpStatus.NOT_FOUND);
        }
        return toVO(account);
    }

    @Override
    @Transactional
    public Long save(WechatAccountSaveRequest request)
    {
        WechatAccount account = new WechatAccount();
        BeanUtils.copyProperties(request, account);
        if (request.getId() == null)
        {
            wechatAccountMapper.insert(account);
        }
        else
        {
            WechatAccount existing = wechatAccountMapper.selectById(request.getId());
            if (existing == null)
            {
                throw new ServiceException("wechat account not found", HttpStatus.NOT_FOUND);
            }
            wechatAccountMapper.updateById(account);
        }
        return account.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        if (wechatAccountMapper.deleteById(id) == 0)
        {
            throw new ServiceException("wechat account not found", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public WechatAccount getEnabledAccount(Long accountId)
    {
        WechatAccount account = wechatAccountMapper.selectOne(
                new LambdaQueryWrapper<WechatAccount>().eq(WechatAccount::getId, accountId).eq(WechatAccount::getEnabled, 1));
        if (account == null)
        {
            throw new ServiceException("wechat account not found or disabled", HttpStatus.NOT_FOUND);
        }
        return account;
    }

    @Override
    public void testConnection(Long id)
    {
        wechatTokenService.getAccessToken(id);
    }

    private WechatAccountVO toVO(WechatAccount account)
    {
        WechatAccountVO vo = new WechatAccountVO();
        BeanUtils.copyProperties(account, vo);
        return vo;
    }
}
