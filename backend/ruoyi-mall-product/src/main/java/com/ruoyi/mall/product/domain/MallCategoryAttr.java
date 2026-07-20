package com.ruoyi.mall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_category_attr")
public class MallCategoryAttr
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long attrId;
    private String attrType;
    private String required;
    private Integer sort;
}
