package com.ruoyi.blog.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AiFeatureModuleConfigItemVO
{
    private String moduleCode;

    private Boolean inherited;

    private Long providerId;

    private String textModel;

    private String visionModel;

    private BigDecimal temperature;

    private String remark;
}
