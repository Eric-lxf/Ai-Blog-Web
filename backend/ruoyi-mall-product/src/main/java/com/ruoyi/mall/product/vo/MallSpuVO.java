package com.ruoyi.mall.product.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MallSpuVO
{
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String name;
    private String subtitle;
    private String mainImage;
    private String detailHtml;
    private String status;
    private Integer sort;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;
}
