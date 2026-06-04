package com.ruoyi.blog.vo.analytics;

import lombok.Data;

@Data
public class AnalyticsTrendVO
{
    private String label;
    private Long pv;
    private Long uv;
    private Long comments;
    private Long likes;
    private Long newUsers;
}
