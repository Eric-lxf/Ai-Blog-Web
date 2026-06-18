package com.ruoyi.wechat.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class WechatTemplateSendRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "openId is required")
    private String openId;

    @NotBlank(message = "templateId is required")
    private String templateId;

    private String url;

    /** 模板字段，如 {"first":{"value":"..."}} */
    @NotNull(message = "data is required")
    private Map<String, Object> data;
}
