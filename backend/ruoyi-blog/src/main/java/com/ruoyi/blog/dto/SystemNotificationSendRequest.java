package com.ruoyi.blog.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemNotificationSendRequest
{
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题过长")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容过长")
    private String content;

    @Size(max = 500, message = "链接过长")
    private String linkUrl;

    /** 为空表示全部正常用户 */
    private List<Long> userIds;
}
