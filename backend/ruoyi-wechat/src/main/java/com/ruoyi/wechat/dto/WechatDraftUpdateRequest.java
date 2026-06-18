package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatDraftUpdateRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "mediaId is required")
    private String mediaId;

    @Min(0)
    private Integer index = 0;

    @NotBlank(message = "title is required")
    private String title;

    private String author;

    private String digest;

    @NotBlank(message = "content is required")
    private String content;

    private String thumbMediaId;

    private String contentSourceUrl;
}
