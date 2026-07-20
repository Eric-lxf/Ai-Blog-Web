package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MallSpuPageQuery
{
    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    private String name;
    /** 公开列表关键词；与 name 任一有值即按商品名称模糊匹配 */
    private String keyword;
    private String status;
    private Long categoryId;
    private Long brandId;
    /** 排序：latest（默认）| price；非法值回退默认 */
    private String sort;
}
