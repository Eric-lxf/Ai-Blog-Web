package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatAutoReplySaveRequest
{
    private Long id;

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "replyType is required")
    private String replyType;

    private String keyword;

    @NotBlank(message = "content is required")
    private String content;

    private Integer enabled = 1;
    private Integer matchType = 1;
}
