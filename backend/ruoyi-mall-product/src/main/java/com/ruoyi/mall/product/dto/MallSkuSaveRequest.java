package com.ruoyi.mall.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallSkuSaveRequest
{
    private Long id;

    @NotBlank(message = "SKU编码不能为空")
    @Size(max = 64, message = "SKU编码长度不能超过64")
    private String skuCode;

    private String specsJson;

    @NotNull(message = "SKU价格不能为空")
    @DecimalMin(value = "0.00", message = "SKU价格不能小于0")
    private BigDecimal price;

    @NotNull(message = "SKU库存不能为空")
    @Min(value = 0, message = "SKU库存不能小于0")
    private Integer stock;

    private String status;
    private String remark;
}
