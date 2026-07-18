package com.ruoyi.mall.trade.dto;

import lombok.Data;

@Data
public class OrderPageQuery
{
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String status;
}
