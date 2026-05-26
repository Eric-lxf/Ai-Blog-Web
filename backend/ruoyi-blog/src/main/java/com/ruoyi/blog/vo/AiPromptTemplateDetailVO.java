package com.ruoyi.blog.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AiPromptTemplateDetailVO
{

    private Long id;
    private String templateName;
    private String sceneType;
    private String systemPrompt;
    private String modelName;
    private BigDecimal temperature;
}
