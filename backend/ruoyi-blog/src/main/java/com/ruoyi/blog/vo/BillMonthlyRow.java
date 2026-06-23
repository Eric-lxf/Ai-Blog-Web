package com.ruoyi.blog.vo;

import java.math.BigDecimal;

import lombok.Data;

/** 月度分类消费行（Mapper 投影，不直接返回给前端）。 */
@Data
public class BillMonthlyRow
{

    /** 格式：yyyy-MM */
    private String month;
    private String category;
    private BigDecimal amount;
}