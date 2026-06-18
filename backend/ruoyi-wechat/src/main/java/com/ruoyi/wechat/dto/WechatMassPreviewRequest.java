package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class WechatMassPreviewRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "msgType is required")
    private String msgType;

    private String content;

    private String mediaId;
}
