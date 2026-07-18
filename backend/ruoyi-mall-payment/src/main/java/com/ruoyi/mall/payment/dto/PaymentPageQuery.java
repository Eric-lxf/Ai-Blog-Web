package com.ruoyi.mall.payment.dto;

import lombok.Data;

@Data
public class PaymentPageQuery
{
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String payNo;
    private String orderNo;
    private String channel;
    private String status;
}
