package com.ruoyi.blog.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NotificationVO
{
    private Long id;
    private String type;
    private String title;
    private String content;
    private String linkUrl;
    private String bizType;
    private Long bizId;
    private Boolean isRead;
    private LocalDateTime createTime;
}
