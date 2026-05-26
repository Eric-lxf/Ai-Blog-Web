package com.ruoyi.blog.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleSaveRequest
{

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过500")
    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    private String coverImage;
    private Long categoryId;
    /** 0-草稿, 1-已发布 */
    private Integer status;
    private List<Long> tagIds;
    private List<String> tagNames;
}
