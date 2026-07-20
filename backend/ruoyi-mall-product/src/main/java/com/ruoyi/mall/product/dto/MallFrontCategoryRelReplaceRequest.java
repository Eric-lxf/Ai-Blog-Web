package com.ruoyi.mall.product.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MallFrontCategoryRelReplaceRequest
{
    private List<Long> backCategoryIds = new ArrayList<>();
}
