package com.ruoyi.mall.trade.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.trade.domain.MallAddress;
import com.ruoyi.mall.trade.dto.AddressSaveRequest;
import com.ruoyi.mall.trade.mapper.MallAddressMapper;
import com.ruoyi.mall.trade.service.MallAddressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallAddressServiceImpl implements MallAddressService
{
    private static final String DEFAULT_YES = "1";
    private static final String DEFAULT_NO = "0";

    private final MallAddressMapper mallAddressMapper;

    @Override
    public List<MallAddress> listMine()
    {
        Long userId = SecurityUtils.getUserId();
        return mallAddressMapper.selectList(new LambdaQueryWrapper<MallAddress>()
                .eq(MallAddress::getUserId, userId)
                .orderByDesc(MallAddress::getIsDefault)
                .orderByDesc(MallAddress::getUpdateTime)
                .orderByDesc(MallAddress::getId));
    }

    @Override
    public MallAddress getMine(Long id)
    {
        return requireMine(id, SecurityUtils.getUserId());
    }

    @Override
    @Transactional
    public Long create(AddressSaveRequest request)
    {
        Long userId = SecurityUtils.getUserId();
        MallAddress address = new MallAddress();
        BeanUtils.copyProperties(request, address);
        address.setId(null);
        address.setUserId(userId);
        address.setCreateBy(SecurityUtils.getUsername());
        address.setUpdateBy(SecurityUtils.getUsername());
        address.setIsDefault(DEFAULT_YES.equals(request.getIsDefault()) ? DEFAULT_YES : DEFAULT_NO);
        if (DEFAULT_YES.equals(address.getIsDefault()))
        {
            clearDefault(userId);
        }
        mallAddressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AddressSaveRequest request)
    {
        Long userId = SecurityUtils.getUserId();
        requireMine(id, userId);
        MallAddress address = new MallAddress();
        BeanUtils.copyProperties(request, address);
        address.setId(id);
        address.setUserId(userId);
        address.setUpdateBy(SecurityUtils.getUsername());
        address.setIsDefault(DEFAULT_YES.equals(request.getIsDefault()) ? DEFAULT_YES : DEFAULT_NO);
        if (DEFAULT_YES.equals(address.getIsDefault()))
        {
            clearDefault(userId);
        }
        mallAddressMapper.updateById(address);
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        Long userId = SecurityUtils.getUserId();
        requireMine(id, userId);
        mallAddressMapper.deleteById(id);
    }

    private MallAddress requireMine(Long id, Long userId)
    {
        MallAddress address = mallAddressMapper.selectOne(new LambdaQueryWrapper<MallAddress>()
                .eq(MallAddress::getId, id)
                .eq(MallAddress::getUserId, userId));
        if (address == null)
        {
            throw new ServiceException("收货地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId)
    {
        MallAddress update = new MallAddress();
        update.setIsDefault(DEFAULT_NO);
        mallAddressMapper.update(update, new LambdaUpdateWrapper<MallAddress>()
                .eq(MallAddress::getUserId, userId)
                .eq(MallAddress::getIsDefault, DEFAULT_YES));
    }
}
