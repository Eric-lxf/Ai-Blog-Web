package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallCategorySaveRequest
{
    private Long id;
    private Long parentId;

    @NotBlank(message = "类目名称不能为空")
    @Size(max = 64, message = "类目名称长度不能超过64")
    private String name;

    private Integer sort;
    private String status;
    private String icon;
    private String remark;
}
