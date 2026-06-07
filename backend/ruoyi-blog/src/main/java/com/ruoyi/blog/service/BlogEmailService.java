package com.ruoyi.blog.service;

public interface BlogEmailService
{
    void sendNotificationEmailAsync(Long userId, String toEmail, String subject, String htmlBody);

    void retryPendingEmails();
}
