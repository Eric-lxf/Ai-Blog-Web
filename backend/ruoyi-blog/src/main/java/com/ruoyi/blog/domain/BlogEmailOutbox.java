package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_email_outbox")
public class BlogEmailOutbox
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String toEmail;
    private String subject;
    private String body;
    private Integer status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime sentTime;
}
