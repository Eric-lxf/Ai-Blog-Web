package com.ruoyi.mall.product.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallSku;
import com.ruoyi.mall.product.domain.MallSpu;
import com.ruoyi.mall.product.mapper.MallSkuMapper;
import com.ruoyi.mall.product.mapper.MallSpuMapper;
import com.ruoyi.mall.product.service.MallSkuService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallSkuServiceImpl implements MallSkuService
{
    private final MallSkuMapper mallSkuMapper;
    private final MallSpuMapper mallSpuMapper;

    @Override
    public MallSku getEnabledSku(Long skuId)
    {
        MallSku sku = mallSkuMapper.selectOne(new LambdaQueryWrapper<MallSku>()
                .eq(MallSku::getId, skuId)
                .eq(MallSku::getStatus, MallProductConstants.STATUS_NORMAL)
                .eq(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
        if (sku == null)
        {
            return null;
        }
        MallSpu spu = mallSpuMapper.selectOne(new LambdaQueryWrapper<MallSpu>()
                .eq(MallSpu::getId, sku.getSpuId())
                .eq(MallSpu::getStatus, MallProductConstants.SPU_STATUS_ON)
                .eq(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
        return spu == null ? null : sku;
    }
}
