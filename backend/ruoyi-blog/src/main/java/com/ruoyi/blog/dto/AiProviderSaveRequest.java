package com.ruoyi.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiProviderSaveRequest
{
    private Long id;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "厂商类型不能为空")
    private String providerType;

    /** 新增必填；编辑留空表示不修改 */
    private String apiKey;

    @NotBlank(message = "Base URL 不能为空")
    private String baseUrl;

    @NotBlank(message = "默认模型不能为空")
    private String defaultModel;

    private String visionModel;

    @NotNull
    @Min(10)
    @Max(600)
    private Integer timeoutSeconds = 300;

    @NotNull
    private Integer enabled = 1;

    private String remark;
}
