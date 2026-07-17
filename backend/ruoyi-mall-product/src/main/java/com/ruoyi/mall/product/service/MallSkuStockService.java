package com.ruoyi.mall.product.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.product.service.dto.MallSkuInfo;

public interface MallSkuStockService
{
    boolean deductStock(Long skuId, int qty);

    default boolean deductStock(Long skuId, Integer qty)
    {
        if (qty == null)
        {
            throw new ServiceException("商品数量不正确");
        }
        return deductStock(skuId, qty.intValue());
    }

    void restoreStock(Long skuId, int qty);

    default void restoreStock(Long skuId, Integer qty)
    {
        if (qty == null)
        {
            throw new ServiceException("商品数量不正确");
        }
        restoreStock(skuId, qty.intValue());
    }

    MallSkuInfo getEnabledSku(Long skuId);
}
