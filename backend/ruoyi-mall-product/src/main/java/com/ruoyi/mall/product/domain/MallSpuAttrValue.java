package com.ruoyi.mall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_spu_attr_value")
public class MallSpuAttrValue
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    private Long attrId;
    private String value;
}
