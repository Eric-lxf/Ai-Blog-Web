package com.ruoyi.blog.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AiTaskVO
{

    private Long id;
    private String taskType;
    private Long targetArticleId;
    private String intermediateData;
    private String resultContent;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
