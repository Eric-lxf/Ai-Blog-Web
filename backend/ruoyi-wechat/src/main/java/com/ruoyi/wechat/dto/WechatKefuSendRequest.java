package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class WechatKefuSendRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "content is required")
    @Size(max = 600)
    private String content;
}
