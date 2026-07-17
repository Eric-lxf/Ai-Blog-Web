package com.ruoyi.mall.payment.gateway;

import lombok.Data;

@Data
public class NotifyResult
{
    private String payNo;
    private boolean success;
    private String channelTradeNo;
}
