package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatAccountSaveRequest
{
    private Long id;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "appId is required")
    private String appId;

    @NotBlank(message = "appSecret is required")
    private String appSecret;

    @NotBlank(message = "token is required")
    private String token;

    private String aesKey;
    private Integer enabled = 1;
}
