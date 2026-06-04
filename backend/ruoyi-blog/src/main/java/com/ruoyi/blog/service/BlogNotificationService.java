package com.ruoyi.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogNotificationPreference;
import com.ruoyi.blog.dto.NotificationPageQuery;
import com.ruoyi.blog.dto.SystemNotificationSendRequest;
import com.ruoyi.blog.vo.NotificationVO;

public interface BlogNotificationService
{
    void onCommentApproved(Long commentId);

    Page<NotificationVO> page(Long userId, NotificationPageQuery query);

    long countUnread(Long userId);

    void markRead(Long userId, Long notificationId);

    void markReadAll(Long userId);

    BlogNotificationPreference getPreference(Long userId);

    void updatePreference(Long userId, BlogNotificationPreference preference);

    void sendSystemNotification(SystemNotificationSendRequest request);
}
