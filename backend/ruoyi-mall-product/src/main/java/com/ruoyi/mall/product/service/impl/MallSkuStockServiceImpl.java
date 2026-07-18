package com.ruoyi.mall.product.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallSku;
import com.ruoyi.mall.product.domain.MallSpu;
import com.ruoyi.mall.product.mapper.MallSkuMapper;
import com.ruoyi.mall.product.mapper.MallSpuMapper;
import com.ruoyi.mall.product.service.MallSkuStockService;
import com.ruoyi.mall.product.service.dto.MallSkuInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallSkuStockServiceImpl implements MallSkuStockService
{
    private final MallSkuMapper mallSkuMapper;
    private final MallSpuMapper mallSpuMapper;

    @Override
    @Transactional
    public boolean deductStock(Long skuId, int qty)
    {
        validateQty(qty);
        return mallSkuMapper.deductStock(skuId, qty) > 0;
    }

    @Override
    @Transactional
    public void restoreStock(Long skuId, int qty)
    {
        validateQty(qty);
        if (mallSkuMapper.restoreStock(skuId, qty) == 0)
        {
            throw new ServiceException("SKU不存在，库存回补失败", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public MallSkuInfo getEnabledSku(Long skuId)
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
        if (spu == null)
        {
            return null;
        }
        MallSkuInfo info = new MallSkuInfo();
        info.setSkuId(sku.getId());
        info.setSpuId(sku.getSpuId());
        info.setSpuName(spu.getName());
        info.setSkuCode(sku.getSkuCode());
        info.setSkuSpecs(sku.getSpecsJson());
        info.setImage(spu.getMainImage());
        info.setPrice(sku.getPrice());
        info.setStock(sku.getStock());
        return info;
    }

    private void validateQty(int qty)
    {
        if (qty <= 0)
        {
            throw new ServiceException("库存数量必须大于0", HttpStatus.BAD_REQUEST);
        }
    }
}
