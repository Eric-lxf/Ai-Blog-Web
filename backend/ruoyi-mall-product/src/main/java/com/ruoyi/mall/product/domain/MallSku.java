package com.ruoyi.mall.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_sku")
public class MallSku
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    private String skuCode;
    private String specsJson;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;
    private String delFlag;
}
