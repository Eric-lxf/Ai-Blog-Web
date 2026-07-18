package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MallSpuPublishRequest
{
    @NotBlank(message = "商品状态不能为空")
    private String status;
}
