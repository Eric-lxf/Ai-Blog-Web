package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatFreePublishBatchRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @Min(0)
    private Integer offset = 0;

    @Min(1)
    @Max(20)
    private Integer count = 10;

    /** 1 不返回 content，0 返回 content */
    private Integer noContent = 1;
}
