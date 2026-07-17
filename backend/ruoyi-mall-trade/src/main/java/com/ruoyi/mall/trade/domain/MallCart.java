package com.ruoyi.mall.trade.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_cart")
public class MallCart
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private String checked;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
