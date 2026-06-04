package com.ruoyi.blog.vo.analytics;

import lombok.Data;

@Data
public class AnalyticsHotArticleVO
{
    private Long articleId;
    private String title;
    private Long viewCount;
    private Long commentCount;
    private Long likeCount;
    private Long periodPv;
}
