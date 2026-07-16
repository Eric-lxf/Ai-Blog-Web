package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.dto.SystemNotificationSendRequest;
import com.ruoyi.blog.service.BlogNotificationService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/notification/admin")
@RequiredArgsConstructor
public class BlogNotificationAdminController extends BlogControllerSupport
{
    private final BlogNotificationService blogNotificationService;

    @PreAuthorize("@ss.hasPermi('blog:notification:send')")
    @Log(title = "系统通知", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody SystemNotificationSendRequest request)
    {
        blogNotificationService.sendSystemNotification(request);
        return AjaxResult.success();
    }
}
