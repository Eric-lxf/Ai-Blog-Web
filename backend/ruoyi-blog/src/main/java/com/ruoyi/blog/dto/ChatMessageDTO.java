package com.ruoyi.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageDTO
{

    @NotBlank(message = "消息角色不能为空")
    private String role;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
