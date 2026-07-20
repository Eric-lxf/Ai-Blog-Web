package com.ruoyi.mall.product.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MallAttrTemplateVO
{
    private List<MallAttrVO> saleAttrs = new ArrayList<>();
    private List<MallAttrVO> descAttrs = new ArrayList<>();
}
