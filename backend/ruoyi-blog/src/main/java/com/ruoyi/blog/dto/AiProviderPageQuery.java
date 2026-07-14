package com.ruoyi.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AiProviderPageQuery
{
    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    private String keyword;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private String providerType;
}
