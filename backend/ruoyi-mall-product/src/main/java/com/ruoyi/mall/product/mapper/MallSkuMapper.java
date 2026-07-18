package com.ruoyi.mall.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.mall.product.domain.MallSku;

@Mapper
public interface MallSkuMapper extends BaseMapper<MallSku>
{
    @Update("UPDATE mall_sku SET stock = stock - #{qty}, update_time = NOW() "
            + "WHERE id = #{skuId} AND stock >= #{qty} AND status = '0' AND del_flag = '0'")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    @Update("UPDATE mall_sku SET stock = stock + #{qty}, update_time = NOW() WHERE id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("qty") int qty);
}
