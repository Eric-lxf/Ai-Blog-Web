package com.ruoyi.blog.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ArticleBriefVO
{

    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private List<String> tagNames;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
