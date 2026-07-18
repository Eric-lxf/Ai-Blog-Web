package com.ruoyi.mall.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCreateRequest
{
    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotBlank(message = "支付渠道不能为空")
    private String channel;
}
