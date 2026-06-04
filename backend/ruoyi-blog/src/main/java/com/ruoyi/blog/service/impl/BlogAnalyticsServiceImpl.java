package com.ruoyi.blog.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ruoyi.blog.mapper.BlogAnalyticsMapper;
import com.ruoyi.blog.mapper.BlogVisitLogMapper;
import com.ruoyi.blog.service.BlogAnalyticsService;
import com.ruoyi.blog.vo.analytics.AnalyticsHotArticleVO;
import com.ruoyi.blog.vo.analytics.AnalyticsRankVO;
import com.ruoyi.blog.vo.analytics.AnalyticsSummaryVO;
import com.ruoyi.blog.vo.analytics.AnalyticsTrendVO;
import com.ruoyi.blog.vo.analytics.BlogAnalyticsDashboardVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogAnalyticsServiceImpl implements BlogAnalyticsService
{
    private static final int MAX_DAYS = 90;
    private static final int RANK_LIMIT = 10;
    private static final int HOT_LIMIT = 10;

    private final BlogVisitLogMapper visitLogMapper;
    private final BlogAnalyticsMapper analyticsMapper;

    @Override
    public BlogAnalyticsDashboardVO dashboard(int days)
    {
        int rangeDays = days < 1 ? 7 : Math.min(days, MAX_DAYS);
        LocalDateTime start = LocalDate.now().minusDays(rangeDays - 1L).atStartOfDay();

        AnalyticsSummaryVO summary = buildSummary(start);
        List<AnalyticsTrendVO> trend = buildTrend(start, rangeDays);
        List<AnalyticsHotArticleVO> hotArticles = analyticsMapper.hotArticles(start, HOT_LIMIT);
        List<AnalyticsRankVO> sources = visitLogMapper.rankRefererSince(start, RANK_LIMIT);
        List<AnalyticsRankVO> regions = visitLogMapper.rankRegionSince(start, RANK_LIMIT);

        BlogAnalyticsDashboardVO vo = new BlogAnalyticsDashboardVO();
        vo.setSummary(summary);
        vo.setTrend(trend);
        vo.setHotArticles(hotArticles);
        vo.setSources(sources);
        vo.setRegions(regions);
        return vo;
    }

    private AnalyticsSummaryVO buildSummary(LocalDateTime start)
    {
        AnalyticsSummaryVO summary = new AnalyticsSummaryVO();
        summary.setPv(visitLogMapper.countPvSince(start));
        summary.setUv(visitLogMapper.countUvSince(start));
        summary.setReadCount(visitLogMapper.countArticleReadsSince(start));
        summary.setLikeCount(analyticsMapper.countCommentLikesSince(start));
        summary.setCommentCount(analyticsMapper.countApprovedCommentsSince(start));
        summary.setNewUsers(analyticsMapper.countNewUsersSince(start));
        summary.setTotalReadCount(analyticsMapper.sumArticleViewCount());
        return summary;
    }

    private List<AnalyticsTrendVO> buildTrend(LocalDateTime start, int rangeDays)
    {
        Map<String, AnalyticsTrendVO> map = new HashMap<>();
        for (int i = 0; i < rangeDays; i++)
        {
            LocalDate day = LocalDate.now().minusDays(rangeDays - 1L - i);
            String label = day.toString();
            AnalyticsTrendVO row = new AnalyticsTrendVO();
            row.setLabel(label);
            row.setPv(0L);
            row.setUv(0L);
            row.setComments(0L);
            row.setLikes(0L);
            row.setNewUsers(0L);
            map.put(label, row);
        }
        mergeTrend(map, visitLogMapper.trendPvUvSince(start));
        mergeTrend(map, analyticsMapper.trendCommentsSince(start));
        mergeTrend(map, analyticsMapper.trendLikesSince(start));
        mergeTrend(map, analyticsMapper.trendNewUsersSince(start));
        return new ArrayList<>(map.values());
    }

    private void mergeTrend(Map<String, AnalyticsTrendVO> map, List<AnalyticsTrendVO> partial)
    {
        if (partial == null)
        {
            return;
        }
        for (AnalyticsTrendVO item : partial)
        {
            if (item == null || item.getLabel() == null)
            {
                continue;
            }
            AnalyticsTrendVO target = map.get(item.getLabel());
            if (target == null)
            {
                target = new AnalyticsTrendVO();
                target.setLabel(item.getLabel());
                target.setPv(0L);
                target.setUv(0L);
                target.setComments(0L);
                target.setLikes(0L);
                target.setNewUsers(0L);
                map.put(item.getLabel(), target);
            }
            if (item.getPv() != null)
            {
                target.setPv(item.getPv());
            }
            if (item.getUv() != null)
            {
                target.setUv(item.getUv());
            }
            if (item.getComments() != null)
            {
                target.setComments(item.getComments());
            }
            if (item.getLikes() != null)
            {
                target.setLikes(item.getLikes());
            }
            if (item.getNewUsers() != null)
            {
                target.setNewUsers(item.getNewUsers());
            }
        }
    }
}
