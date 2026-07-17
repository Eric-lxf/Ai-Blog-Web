package com.ruoyi.mall.trade.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CartUpdateRequest
{
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    private String checked;
}
