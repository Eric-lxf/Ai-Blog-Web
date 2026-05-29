package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_user_notification")
public class BlogUserNotification
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String linkUrl;
    private String bizType;
    private Long bizId;
    private Integer isRead;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
