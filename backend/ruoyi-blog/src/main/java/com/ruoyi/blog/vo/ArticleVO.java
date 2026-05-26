package com.ruoyi.blog.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ArticleVO
{

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private List<Long> tagIds;
    private List<String> tagNames;
    private Integer status;
    private Integer isAiGenerated;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
