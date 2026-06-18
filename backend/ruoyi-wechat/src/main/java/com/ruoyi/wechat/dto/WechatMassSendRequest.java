package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class WechatMassSendRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "msgType is required")
    private String msgType;

    private String content;

    private String mediaId;

    /** 是否全员发送 */
    @NotNull(message = "isToAll is required")
    private Boolean isToAll;

    /** 按标签发送时的微信 tag_id（isToAll=false 时必填） */
    private Integer wechatTagId;
}
