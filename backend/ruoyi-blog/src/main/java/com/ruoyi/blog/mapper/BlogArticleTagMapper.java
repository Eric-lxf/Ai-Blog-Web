package com.ruoyi.blog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.blog.domain.BlogArticleTag;
import com.ruoyi.blog.domain.BlogTag;
import com.ruoyi.blog.dto.ArticleTagRow;

@Mapper
public interface BlogArticleTagMapper extends BaseMapper<BlogArticleTag>
{

    @Delete("DELETE FROM blog_article_tag WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Insert("""
            <script>
            INSERT INTO blog_article_tag (article_id, tag_id) VALUES
            <foreach collection='tagIds' item='tagId' separator=','>
              (#{articleId}, #{tagId})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);

    @Select("""
            SELECT t.id, t.name, t.create_time
            FROM blog_tag t
            INNER JOIN blog_article_tag at ON t.id = at.tag_id
            WHERE at.article_id = #{articleId}
            """)
    List<BlogTag> selectTagsByArticleId(@Param("articleId") Long articleId);

    @Select("""
            <script>
            SELECT at.article_id, t.id, t.name, t.create_time
            FROM blog_tag t
            INNER JOIN blog_article_tag at ON t.id = at.tag_id
            WHERE at.article_id IN
            <foreach collection="articleIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    List<ArticleTagRow> selectTagsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
