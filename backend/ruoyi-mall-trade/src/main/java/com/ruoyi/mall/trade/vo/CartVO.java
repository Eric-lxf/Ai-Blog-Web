package com.ruoyi.mall.trade.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CartVO
{
    private Long id;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private String checked;
    private Long spuId;
    private String spuName;
    private String skuCode;
    private String skuSpecs;
    private String image;
    private BigDecimal price;
    private Integer stock;
    private Boolean skuEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
