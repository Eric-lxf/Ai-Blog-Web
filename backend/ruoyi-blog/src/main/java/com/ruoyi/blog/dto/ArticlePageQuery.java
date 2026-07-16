package com.ruoyi.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ArticlePageQuery
{

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    private String keyword;
    private Integer status;
    private Long categoryId;

    /** latest | hot，默认 latest */
    private String sort;
}
