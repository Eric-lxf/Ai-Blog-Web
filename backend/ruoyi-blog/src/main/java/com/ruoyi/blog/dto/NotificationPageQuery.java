package com.ruoyi.blog.dto;

import lombok.Data;

@Data
public class NotificationPageQuery
{
    private Integer pageNum;
    private Integer pageSize;
    private String type;
    private Integer isRead;
}
