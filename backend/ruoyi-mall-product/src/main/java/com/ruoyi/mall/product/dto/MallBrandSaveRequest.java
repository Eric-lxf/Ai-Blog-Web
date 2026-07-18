package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallBrandSaveRequest
{
    private Long id;

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 64, message = "品牌名称长度不能超过64")
    private String name;

    private String logo;
    private Integer sort;
    private String status;
    private String remark;
}
