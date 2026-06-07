package com.ruoyi.blog.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.blog.domain.BlogVisitLog;
import com.ruoyi.blog.vo.analytics.AnalyticsRankVO;
import com.ruoyi.blog.vo.analytics.AnalyticsTrendVO;

@Mapper
public interface BlogVisitLogMapper extends BaseMapper<BlogVisitLog>
{
    @Select("SELECT COUNT(*) FROM blog_visit_log WHERE create_time >= #{start}")
    long countPvSince(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(DISTINCT visitor_key) FROM blog_visit_log WHERE create_time >= #{start}")
    long countUvSince(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(*) FROM blog_visit_log WHERE page_type = 'ARTICLE' AND create_time >= #{start}")
    long countArticleReadsSince(@Param("start") LocalDateTime start);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT visitor_key) AS uv
            FROM blog_visit_log
            WHERE create_time >= #{start}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY label
            """)
    List<AnalyticsTrendVO> trendPvUvSince(@Param("start") LocalDateTime start);

    @Select("""
            SELECT IFNULL(referer_host, '直接访问') AS name, COUNT(*) AS count
            FROM blog_visit_log
            WHERE create_time >= #{start}
            GROUP BY referer_host
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<AnalyticsRankVO> rankRefererSince(@Param("start") LocalDateTime start, @Param("limit") int limit);

    @Select("""
            SELECT IFNULL(NULLIF(region, ''), '未知') AS name, COUNT(*) AS count
            FROM blog_visit_log
            WHERE create_time >= #{start}
            GROUP BY region
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<AnalyticsRankVO> rankRegionSince(@Param("start") LocalDateTime start, @Param("limit") int limit);
}
