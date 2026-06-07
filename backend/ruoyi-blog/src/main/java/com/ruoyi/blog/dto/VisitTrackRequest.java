package com.ruoyi.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VisitTrackRequest
{
    @NotBlank(message = "页面类型不能为空")
    private String pageType;

    private Long articleId;

    private String visitorId;
}
