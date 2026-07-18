package com.ruoyi.mall.product.dto;

import lombok.Data;

@Data
public class MallCategoryQuery
{
    private Boolean tree;
    private Long parentId;
    private String name;
    private String status;
}
