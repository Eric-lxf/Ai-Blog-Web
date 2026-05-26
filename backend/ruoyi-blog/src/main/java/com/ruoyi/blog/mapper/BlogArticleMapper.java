package com.ruoyi.blog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.blog.domain.BlogArticle;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle>
{

    @Update("UPDATE blog_article SET view_count = IFNULL(view_count, 0) + 1 WHERE id = #{id} AND is_deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
