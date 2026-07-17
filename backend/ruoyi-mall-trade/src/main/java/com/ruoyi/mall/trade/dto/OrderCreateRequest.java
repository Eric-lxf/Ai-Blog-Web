package com.ruoyi.mall.trade.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest
{
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    private List<Long> cartIds;

    @Valid
    private List<OrderCreateItemRequest> items;
}
