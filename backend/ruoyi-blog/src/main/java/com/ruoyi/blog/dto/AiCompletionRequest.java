package com.ruoyi.blog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiCompletionRequest
{

    private String scene;

    @Size(max = 32000, message = "提示词过长")
    private String prompt;

    private String customSystemPrompt;

    private BigDecimal temperature;
}
