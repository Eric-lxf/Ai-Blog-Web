package com.ruoyi.mall.product.service.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MallSkuInfo
{
    private Long skuId;
    private Long spuId;
    private String spuName;
    private String skuCode;
    private String skuSpecs;
    private String image;
    private BigDecimal price;
    private Integer stock;
}
