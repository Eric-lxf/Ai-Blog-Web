package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class WechatTagSaveRequest
{
    private Long id;

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "name is required")
    @Size(max = 30)
    private String name;
}
