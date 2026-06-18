package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatDraftSaveRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "title is required")
    private String title;

    private String author;

    private String digest;

    @NotBlank(message = "content is required")
    private String content;

    @NotBlank(message = "thumbMediaId is required")
    private String thumbMediaId;

    private String contentSourceUrl;
}
