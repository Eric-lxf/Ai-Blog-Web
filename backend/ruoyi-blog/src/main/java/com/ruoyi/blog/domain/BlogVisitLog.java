package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_visit_log")
public class BlogVisitLog
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String pageType;

    private Long articleId;

    private String visitorKey;

    private Long userId;

    private String ip;

    private String userAgent;

    private String referer;

    private String refererHost;

    private String region;

    private LocalDateTime createTime;
}
