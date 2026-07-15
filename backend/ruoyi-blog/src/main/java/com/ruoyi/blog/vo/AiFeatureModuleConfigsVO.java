package com.ruoyi.blog.vo;

import java.util.List;

import lombok.Data;

@Data
public class AiFeatureModuleConfigsVO
{
    private List<AiFeatureModuleConfigItemVO> modules;

    private List<AiProviderOptionVO> providerOptions;
}
