package com.ruoyi.blog.service;

import com.ruoyi.blog.vo.analytics.BlogAnalyticsDashboardVO;

public interface BlogAnalyticsService
{
    BlogAnalyticsDashboardVO dashboard(int days);
}
