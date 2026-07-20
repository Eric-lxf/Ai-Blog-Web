package com.ruoyi.mall.product.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MallFrontCategoryTreeVO
{
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private String status;
    private String icon;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MallFrontCategoryTreeVO> children = new ArrayList<>();
}
