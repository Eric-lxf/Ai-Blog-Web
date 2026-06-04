package com.ruoyi.blog.vo.analytics;

import java.util.List;

import lombok.Data;

@Data
public class BlogAnalyticsDashboardVO
{
    private AnalyticsSummaryVO summary;
    private List<AnalyticsTrendVO> trend;
    private List<AnalyticsHotArticleVO> hotArticles;
    private List<AnalyticsRankVO> sources;
    private List<AnalyticsRankVO> regions;
}
