package com.ruoyi.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogNotificationPreference;
import com.ruoyi.blog.dto.NotificationPageQuery;
import com.ruoyi.blog.service.BlogNotificationService;
import com.ruoyi.blog.vo.NotificationVO;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/notification")
@RequiredArgsConstructor
public class BlogNotificationController extends BlogControllerSupport
{
    private final BlogNotificationService blogNotificationService;

    @GetMapping("/list")
    public TableDataInfo list(NotificationPageQuery query)
    {
        Long userId = SecurityUtils.getUserId();
        Page<NotificationVO> page = blogNotificationService.page(userId, query);
        return mpPageTable(page);
    }

    @GetMapping("/unread-count")
    public AjaxResult unreadCount()
    {
        Long userId = SecurityUtils.getUserId();
        AjaxResult result = AjaxResult.success();
        result.put("unreadCount", blogNotificationService.countUnread(userId));
        return result;
    }

    @PostMapping("/{id}/read")
    public AjaxResult markRead(@PathVariable Long id)
    {
        blogNotificationService.markRead(SecurityUtils.getUserId(), id);
        return AjaxResult.success();
    }

    @PostMapping("/read-all")
    public AjaxResult markReadAll()
    {
        blogNotificationService.markReadAll(SecurityUtils.getUserId());
        return AjaxResult.success();
    }

    @GetMapping("/preference")
    public AjaxResult getPreference()
    {
        return AjaxResult.success(blogNotificationService.getPreference(SecurityUtils.getUserId()));
    }

    @PutMapping("/preference")
    public AjaxResult updatePreference(@RequestBody BlogNotificationPreference preference)
    {
        blogNotificationService.updatePreference(SecurityUtils.getUserId(), preference);
        return AjaxResult.success();
    }
}
