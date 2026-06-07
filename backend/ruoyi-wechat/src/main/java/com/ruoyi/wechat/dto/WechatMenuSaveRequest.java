package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatMenuSaveRequest
{
    private Long id;

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "menuJson is required")
    private String menuJson;
}
