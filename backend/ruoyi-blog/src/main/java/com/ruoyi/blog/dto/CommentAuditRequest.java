package com.ruoyi.blog.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentAuditRequest
{
    @NotEmpty(message = "评论ID不能为空")
    private List<Long> ids;

    @NotNull(message = "目标状态不能为空")
    private Integer status;

    private String rejectReason;
}
