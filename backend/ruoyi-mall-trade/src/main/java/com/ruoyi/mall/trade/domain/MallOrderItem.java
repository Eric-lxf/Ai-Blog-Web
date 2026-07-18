package com.ruoyi.mall.trade.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_order_item")
public class MallOrderItem
{
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
}
