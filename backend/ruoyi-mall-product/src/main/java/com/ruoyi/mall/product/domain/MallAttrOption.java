package com.ruoyi.mall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_attr_option")
public class MallAttrOption
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long attrId;
    private String value;
    private Integer sort;
    private String status;
}
