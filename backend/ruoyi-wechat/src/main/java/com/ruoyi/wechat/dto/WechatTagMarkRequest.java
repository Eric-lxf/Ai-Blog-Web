package com.ruoyi.wechat.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class WechatTagMarkRequest
{
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotNull(message = "tagId is required")
    private Long tagId;

    @NotEmpty(message = "openIds is required")
    private List<String> openIds;

    /** true=打标, false=取消打标 */
    @NotNull(message = "mark is required")
    private Boolean mark;
}
