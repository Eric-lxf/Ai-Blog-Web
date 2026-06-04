package com.ruoyi.blog.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.ruoyi.blog.vo.analytics.AnalyticsHotArticleVO;
import com.ruoyi.blog.vo.analytics.AnalyticsTrendVO;

@Mapper
public interface BlogAnalyticsMapper
{
    @Select("SELECT COUNT(*) FROM blog_comment WHERE is_deleted = 0 AND status = 1 AND create_time >= #{start}")
    long countApprovedCommentsSince(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(*) FROM blog_comment_like WHERE create_time >= #{start}")
    long countCommentLikesSince(@Param("start") LocalDateTime start);

    @Select("SELECT COUNT(*) FROM sys_user WHERE del_flag = '0' AND create_time >= #{start}")
    long countNewUsersSince(@Param("start") LocalDateTime start);

    @Select("SELECT IFNULL(SUM(view_count), 0) FROM blog_article WHERE is_deleted = 0 AND status = 1")
    long sumArticleViewCount();

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, COUNT(*) AS comments
            FROM blog_comment
            WHERE is_deleted = 0 AND status = 1 AND create_time >= #{start}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY label
            """)
    List<AnalyticsTrendVO> trendCommentsSince(@Param("start") LocalDateTime start);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, COUNT(*) AS likes
            FROM blog_comment_like
            WHERE create_time >= #{start}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY label
            """)
    List<AnalyticsTrendVO> trendLikesSince(@Param("start") LocalDateTime start);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, COUNT(*) AS newUsers
            FROM sys_user
            WHERE del_flag = '0' AND create_time >= #{start}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY label
            """)
    List<AnalyticsTrendVO> trendNewUsersSince(@Param("start") LocalDateTime start);

    @Select("""
            SELECT a.id AS articleId, a.title,
                   IFNULL(a.view_count, 0) AS viewCount,
                   (SELECT COUNT(*) FROM blog_comment c
                    WHERE c.article_id = a.id AND c.is_deleted = 0 AND c.status = 1) AS commentCount,
                   (SELECT IFNULL(SUM(c.like_count), 0) FROM blog_comment c
                    WHERE c.article_id = a.id AND c.is_deleted = 0) AS likeCount,
                   (SELECT COUNT(*) FROM blog_visit_log v
                    WHERE v.article_id = a.id AND v.page_type = 'ARTICLE' AND v.create_time >= #{start}) AS periodPv
            FROM blog_article a
            WHERE a.is_deleted = 0 AND a.status = 1
            ORDER BY periodPv DESC, a.view_count DESC
            LIMIT #{limit}
            """)
    List<AnalyticsHotArticleVO> hotArticles(@Param("start") LocalDateTime start, @Param("limit") int limit);
}
