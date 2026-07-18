package com.ruoyi.mall.payment.gateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PaymentCreateCmd
{
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String payNo;
    private BigDecimal amount;
    private LocalDateTime expireTime;
}
