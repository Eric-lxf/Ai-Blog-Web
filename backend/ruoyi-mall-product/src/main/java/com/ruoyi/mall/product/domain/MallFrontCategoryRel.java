package com.ruoyi.mall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_front_category_rel")
public class MallFrontCategoryRel
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long frontId;
    private Long backCategoryId;
}
