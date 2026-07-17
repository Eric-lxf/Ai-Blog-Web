package com.ruoyi.mall.trade.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MallOrderPaymentView
{
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String status;
    private BigDecimal payAmount;
    private LocalDateTime expireTime;
}
