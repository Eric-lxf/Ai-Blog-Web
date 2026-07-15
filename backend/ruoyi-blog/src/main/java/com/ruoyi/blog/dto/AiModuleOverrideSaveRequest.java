package com.ruoyi.blog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiModuleOverrideSaveRequest
{
    @NotNull
    private Long providerId;

    private String textModel;

    private String visionModel;

    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private BigDecimal temperature;

    private String remark;
}
