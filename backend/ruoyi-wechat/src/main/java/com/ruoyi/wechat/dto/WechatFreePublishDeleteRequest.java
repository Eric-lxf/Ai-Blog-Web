package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WechatFreePublishDeleteRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "articleId is required")
    private String articleId;

    /** 图文位置，从 1 开始；不填或 0 删除全部 */
    private Integer index;
}
