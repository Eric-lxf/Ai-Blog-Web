package com.ruoyi.blog.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.service.BlogAnalyticsService;
import com.ruoyi.blog.vo.analytics.BlogAnalyticsDashboardVO;
import com.ruoyi.common.core.domain.AjaxResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/analytics")
@RequiredArgsConstructor
public class BlogAnalyticsController extends BlogControllerSupport
{
    private final BlogAnalyticsService blogAnalyticsService;

    @PreAuthorize("@ss.hasPermi('blog:dashboard:view')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard(@RequestParam(defaultValue = "7") int days)
    {
        BlogAnalyticsDashboardVO data = blogAnalyticsService.dashboard(days);
        return AjaxResult.success(data);
    }
}
