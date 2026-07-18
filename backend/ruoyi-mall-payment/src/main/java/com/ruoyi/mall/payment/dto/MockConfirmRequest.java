package com.ruoyi.mall.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockConfirmRequest
{
    @NotBlank(message = "支付单号不能为空")
    private String payNo;
}
