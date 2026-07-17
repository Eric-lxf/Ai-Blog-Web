package com.ruoyi.mall.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MallSpuImageSaveRequest
{
    private Long id;

    @NotBlank(message = "图片地址不能为空")
    private String url;

    private Integer sort;
}
