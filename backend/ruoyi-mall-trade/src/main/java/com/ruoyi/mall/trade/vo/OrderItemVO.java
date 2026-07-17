package com.ruoyi.mall.trade.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrderItemVO
{
    private Long id;
    private Long orderId;
    private Long spuId;
    private Long skuId;
    private String spuName;
    private String skuSpecs;
    private String skuCode;
    private String image;
    private BigDecimal price;
    private Integer quantity;
}
