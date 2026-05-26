package com.ruoyi.blog.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiWriteWizardRequest
{

    @NotBlank(message = "技术主题不能为空")
    private String topic;

    private String audience = "mid";

    private String length = "medium";

    private String title;

    private String summary;

    private List<OutlineNodeDTO> outline = new ArrayList<>();

    private Boolean publish = false;

    private Long categoryId;

    private List<String> tagNames = new ArrayList<>();
}
