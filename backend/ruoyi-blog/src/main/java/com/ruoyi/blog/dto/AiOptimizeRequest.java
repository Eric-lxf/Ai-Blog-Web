package com.ruoyi.blog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiOptimizeRequest
{

    @NotBlank(message = "待优化内容不能为空")
    @Size(max = 50000, message = "待优化内容过长")
    private String content;

    private String scene = "REWRITE";

    private String customSystemPrompt;

    private BigDecimal temperature;
}
