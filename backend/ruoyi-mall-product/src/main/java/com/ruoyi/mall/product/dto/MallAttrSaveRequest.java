package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallAttrSaveRequest
{
    private Long id;

    @NotBlank(message = "属性名称不能为空")
    @Size(max = 64, message = "属性名称长度不能超过64")
    private String name;

    @Size(max = 16, message = "录入类型长度不能超过16")
    private String inputType;

    private Integer sort;
    private String status;
    private String remark;
}
