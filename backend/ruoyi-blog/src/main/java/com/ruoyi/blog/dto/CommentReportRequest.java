package com.ruoyi.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReportRequest
{
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 500, message = "举报原因过长")
    private String reason;
}
