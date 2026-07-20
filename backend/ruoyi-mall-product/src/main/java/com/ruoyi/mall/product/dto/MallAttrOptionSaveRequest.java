package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallAttrOptionSaveRequest
{
    private Long id;

    @NotBlank(message = "选项值不能为空")
    @Size(max = 128, message = "选项值长度不能超过128")
    private String value;

    private Integer sort;
    private String status;
}
