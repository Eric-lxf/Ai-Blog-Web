package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallSpuAttrValueRequest
{
    @NotNull(message = "属性ID不能为空")
    private Long attrId;

    @Size(max = 512, message = "属性值长度不能超过512")
    private String value;
}
