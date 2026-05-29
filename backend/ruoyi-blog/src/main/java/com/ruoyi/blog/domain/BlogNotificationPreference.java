package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_notification_preference")
public class BlogNotificationPreference
{
    @TableId
    private Long userId;
    private Integer enableInApp;
    private Integer enableEmail;
    private Integer enableComment;
    private Integer enableReply;
    private Integer enableSystem;
    private LocalDateTime updateTime;
}
