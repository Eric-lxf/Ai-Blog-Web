package com.ruoyi.blog.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ArticleTagRow
{

    private Long articleId;
    private Long id;
    private String name;
    private LocalDateTime createTime;
}
