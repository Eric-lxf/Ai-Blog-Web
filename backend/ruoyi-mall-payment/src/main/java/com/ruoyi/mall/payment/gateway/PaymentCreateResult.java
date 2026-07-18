package com.ruoyi.mall.payment.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public class PaymentCreateResult
{
    private String channel;
    private String payNo;
    private String payUrl;
    private Boolean mock;
    private Map<String, Object> params = new LinkedHashMap<>();
}
