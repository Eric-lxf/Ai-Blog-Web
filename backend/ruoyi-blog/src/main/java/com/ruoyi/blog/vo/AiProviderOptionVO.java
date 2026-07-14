package com.ruoyi.blog.vo;

import lombok.Data;

@Data
public class AiProviderOptionVO
{
    private Long id;
    private String name;
    private String providerType;
    private String defaultModel;
    private Integer enabled;
}
