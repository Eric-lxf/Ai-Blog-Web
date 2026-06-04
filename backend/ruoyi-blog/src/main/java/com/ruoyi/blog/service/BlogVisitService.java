package com.ruoyi.blog.service;

public interface BlogVisitService
{
    void recordFromRequest(String pageType, Long articleId);

    void record(String pageType, Long articleId, String visitorId);
}
