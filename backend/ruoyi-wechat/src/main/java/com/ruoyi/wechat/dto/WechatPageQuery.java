package com.ruoyi.wechat.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WechatPageQuery
{
    @Min(1)
    private Integer pageNum = 1;

    /** Capped to 100 in service layer; do not use @Max here (breaks legacy pageSize=1000). */
    @Min(1)
    private Integer pageSize = 10;

    private Long accountId;
    private String keyword;
    private Integer status;
    private Long tagId;
}
