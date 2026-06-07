package com.ruoyi.blog.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.constant.BlogNotificationConstants;
import com.ruoyi.blog.domain.BlogEmailOutbox;
import com.ruoyi.blog.mapper.BlogEmailOutboxMapper;
import com.ruoyi.blog.service.BlogEmailService;
import com.ruoyi.blog.service.NotificationConfigService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogEmailServiceImpl implements BlogEmailService
{
    private static final int MAX_RETRY = 3;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final BlogEmailOutboxMapper outboxMapper;
    private final NotificationConfigService notificationConfigService;

    @Value("${blog.notification.from:noreply@localhost}")
    private String fromAddress;

    @Override
    @Async("aiTaskExecutor")
    public void sendNotificationEmailAsync(Long userId, String toEmail, String subject, String htmlBody)
    {
        if (!notificationConfigService.emailEnabled() || !StringUtils.hasText(toEmail))
        {
            return;
        }
        BlogEmailOutbox outbox = new BlogEmailOutbox();
        outbox.setUserId(userId);
        outbox.setToEmail(toEmail.trim());
        outbox.setSubject(subject);
        outbox.setBody(htmlBody);
        outbox.setStatus(BlogNotificationConstants.EMAIL_PENDING);
        outbox.setRetryCount(0);
        outbox.setCreateTime(LocalDateTime.now());
        outboxMapper.insert(outbox);
        deliver(outbox);
    }

    @Override
    @Scheduled(fixedDelayString = "${blog.notification.email.retryDelayMs:300000}")
    public void retryPendingEmails()
    {
        if (!notificationConfigService.emailEnabled())
        {
            return;
        }
        LambdaQueryWrapper<BlogEmailOutbox> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogEmailOutbox::getStatus, BlogNotificationConstants.EMAIL_PENDING)
                .lt(BlogEmailOutbox::getRetryCount, MAX_RETRY)
                .orderByAsc(BlogEmailOutbox::getCreateTime)
                .last("LIMIT 20");
        List<BlogEmailOutbox> pending = outboxMapper.selectList(wrapper);
        for (BlogEmailOutbox row : pending)
        {
            deliver(row);
        }
    }

    private void deliver(BlogEmailOutbox outbox)
    {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null)
        {
            outbox.setStatus(BlogNotificationConstants.EMAIL_FAILED);
            outbox.setErrorMessage("未配置 spring.mail");
            outboxMapper.updateById(outbox);
            return;
        }
        try
        {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(outbox.getToEmail());
            helper.setSubject(outbox.getSubject());
            helper.setText(outbox.getBody(), true);
            mailSender.send(message);
            outbox.setStatus(BlogNotificationConstants.EMAIL_SENT);
            outbox.setSentTime(LocalDateTime.now());
            outbox.setErrorMessage(null);
            outboxMapper.updateById(outbox);
        }
        catch (Exception ex)
        {
            log.warn("Send notification email failed, outboxId={}", outbox.getId(), ex);
            outbox.setRetryCount((outbox.getRetryCount() == null ? 0 : outbox.getRetryCount()) + 1);
            outbox.setErrorMessage(ex.getMessage() != null && ex.getMessage().length() > 480
                    ? ex.getMessage().substring(0, 480)
                    : ex.getMessage());
            if (outbox.getRetryCount() >= MAX_RETRY)
            {
                outbox.setStatus(BlogNotificationConstants.EMAIL_FAILED);
            }
            outboxMapper.updateById(outbox);
        }
    }
}
