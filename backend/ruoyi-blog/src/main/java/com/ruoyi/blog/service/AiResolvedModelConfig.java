package com.ruoyi.blog.service;

import java.math.BigDecimal;

import com.ruoyi.blog.domain.AiProvider;

import lombok.Value;

@Value
public class AiResolvedModelConfig
{
    AiProvider provider;
    String textModel;
    String visionModel;
    BigDecimal temperatureOverride;
    ConfigSource source;

    public enum ConfigSource
    {
        MODULE_OVERRIDE,
        GLOBAL_DEFAULT,
        FIRST_ENABLED
    }
}
