package com.ruoyi.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CommentPageQuery
{
    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    private Long articleId;
    private Integer status;
    private Integer aiStatus;
    private String keyword;
    private String sort = "new";
}
