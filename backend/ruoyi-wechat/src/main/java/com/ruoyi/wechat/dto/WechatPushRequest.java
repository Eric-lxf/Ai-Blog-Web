package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatPushRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotNull(message = "articleId is required")
    private Long articleId;

    @NotBlank(message = "publishMode is required")
    private String publishMode;
}
