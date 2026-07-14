package com.ruoyi.blog.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AiProviderVO
{
    private Long id;
    private String name;
    private String providerType;
    /** 脱敏后的 Key，如 sk-****xxxx */
    private String apiKeyMasked;
    private String baseUrl;
    private String defaultModel;
    private String visionModel;
    private Integer timeoutSeconds;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
