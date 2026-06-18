package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class WechatModuleConfigUpdateRequest
{
    @NotNull
    private Boolean enabled;

    private String defaultAccountId;

    @NotNull
    private Boolean callbackEncrypt;
}
