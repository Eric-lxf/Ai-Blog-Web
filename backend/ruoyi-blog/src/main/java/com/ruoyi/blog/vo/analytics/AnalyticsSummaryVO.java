package com.ruoyi.blog.vo.analytics;

import lombok.Data;

@Data
public class AnalyticsSummaryVO
{
    private long pv;
    private long uv;
    private long readCount;
    private long likeCount;
    private long commentCount;
    private long newUsers;
    private long totalReadCount;
}
