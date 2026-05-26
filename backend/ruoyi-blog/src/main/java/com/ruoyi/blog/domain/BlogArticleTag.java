package com.ruoyi.blog.domain;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_article_tag")
public class BlogArticleTag
{

    private Long articleId;
    private Long tagId;
}
