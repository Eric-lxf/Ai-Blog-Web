package com.ruoyi.mall.product.dto;

import lombok.Data;

@Data
public class MallFrontCategoryQuery
{
    private Boolean tree;
    private Long parentId;
    private String name;
    private String status;
}
