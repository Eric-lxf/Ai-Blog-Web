package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatMaterialDeleteRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "mediaId is required")
    private String mediaId;
}
