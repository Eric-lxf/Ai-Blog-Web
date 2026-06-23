package com.ruoyi.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BillRecognizeRequest
{

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;
}