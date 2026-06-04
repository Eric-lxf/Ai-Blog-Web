package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.dto.VisitTrackRequest;
import com.ruoyi.blog.service.BlogVisitService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import lombok.RequiredArgsConstructor;

@Anonymous
@RestController
@RequestMapping("/public/blog")
@RequiredArgsConstructor
public class PublicAnalyticsController extends BlogControllerSupport
{
    private final BlogVisitService blogVisitService;

    @PostMapping("/track")
    public AjaxResult track(@Valid @RequestBody VisitTrackRequest request)
    {
        String pageType = request.getPageType().trim().toUpperCase();
        blogVisitService.record(pageType, request.getArticleId(), request.getVisitorId());
        return AjaxResult.success();
    }
}
