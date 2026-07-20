package com.ruoyi.mall.product.vo;

import java.util.ArrayList;
import java.util.List;

import com.ruoyi.mall.product.domain.MallAttrOption;

import lombok.Data;

@Data
public class MallAttrVO
{
    private Long id;
    private String name;
    private String inputType;
    private String status;
    private Integer sort;
    private String remark;
    private String attrType;
    private String required;
    private List<MallAttrOption> options = new ArrayList<>();
}
