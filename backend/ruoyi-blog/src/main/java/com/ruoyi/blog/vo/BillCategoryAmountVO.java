package com.ruoyi.blog.vo;

import java.math.BigDecimal;

import lombok.Data;

/** 分类金额汇总（用于饼图数据及 Mapper 投影）。 */
@Data
public class BillCategoryAmountVO
{

    private String name;
    private BigDecimal value;
}