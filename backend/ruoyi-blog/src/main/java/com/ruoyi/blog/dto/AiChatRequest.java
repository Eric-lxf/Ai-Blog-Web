package com.ruoyi.blog.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest
{

    private String scene = "CHAT";

    @NotBlank(message = "请输入对话内容")
    @Size(max = 32000, message = "对话内容过长")
    private String prompt;

    private Boolean includeContext = false;

    private String articleTitle;

    private String articleContent;

    private List<ChatMessageDTO> history = new ArrayList<>();
}
