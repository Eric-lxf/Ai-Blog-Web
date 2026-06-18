package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WechatQrcodeCreateRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "name is required")
    @Size(max = 100)
    private String name;

    /** temp / permanent */
    @NotBlank(message = "qrType is required")
    private String qrType;

    /** int / str */
    @NotBlank(message = "sceneType is required")
    private String sceneType;

    @Min(1)
    @Max(100000)
    private Integer sceneId;

    @Size(max = 64)
    private String sceneStr;

    @Min(60)
    @Max(2592000)
    private Integer expireSeconds;

    @Size(max = 255)
    private String remark;
}
