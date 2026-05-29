package com.ruoyi.blog.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.constant.BlogCommentConstants;
import com.ruoyi.blog.constant.BlogNotificationConstants;
import com.ruoyi.blog.domain.BlogArticle;
import com.ruoyi.blog.domain.BlogComment;
import com.ruoyi.blog.domain.BlogNotificationPreference;
import com.ruoyi.blog.domain.BlogUserNotification;
import com.ruoyi.blog.dto.NotificationPageQuery;
import com.ruoyi.blog.dto.SystemNotificationSendRequest;
import com.ruoyi.blog.enums.BlogNotificationType;
import com.ruoyi.blog.mapper.BlogArticleMapper;
import com.ruoyi.blog.mapper.BlogCommentMapper;
import com.ruoyi.blog.mapper.BlogNotificationPreferenceMapper;
import com.ruoyi.blog.mapper.BlogUserNotificationMapper;
import com.ruoyi.blog.service.BlogEmailService;
import com.ruoyi.blog.service.BlogNotificationService;
import com.ruoyi.blog.service.NotificationConfigService;
import com.ruoyi.blog.vo.NotificationVO;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogNotificationServiceImpl implements BlogNotificationService
{
    private static final int DEDUP_MINUTES = 5;
    private static final int CONTENT_PREVIEW_LEN = 120;

    private final BlogUserNotificationMapper notificationMapper;
    private final BlogNotificationPreferenceMapper preferenceMapper;
    private final BlogCommentMapper commentMapper;
    private final BlogArticleMapper articleMapper;
    private final ISysUserService sysUserService;
    private final NotificationConfigService notificationConfigService;
    private final BlogEmailService blogEmailService;

    @Override
    @Transactional
    public void onCommentApproved(Long commentId)
    {
        if (!notificationConfigService.enabled())
        {
            return;
        }
        BlogComment comment = commentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getStatus(), BlogCommentConstants.STATUS_APPROVED))
        {
            return;
        }
        if (comment.getParentId() == null)
        {
            notifyArticleComment(comment);
        }
        else
        {
            notifyCommentReply(comment);
        }
    }

    @Override
    public Page<NotificationVO> page(Long userId, NotificationPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 50);
        LambdaQueryWrapper<BlogUserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogUserNotification::getUserId, userId);
        if (StringUtils.hasText(query.getType()))
        {
            wrapper.eq(BlogUserNotification::getType, query.getType().trim().toUpperCase());
        }
        if (query.getIsRead() != null)
        {
            wrapper.eq(BlogUserNotification::getIsRead, query.getIsRead());
        }
        wrapper.orderByDesc(BlogUserNotification::getCreateTime);
        Page<BlogUserNotification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<NotificationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    @Override
    public long countUnread(Long userId)
    {
        LambdaQueryWrapper<BlogUserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogUserNotification::getUserId, userId).eq(BlogUserNotification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId)
    {
        BlogUserNotification row = notificationMapper.selectById(notificationId);
        if (row == null || !Objects.equals(row.getUserId(), userId))
        {
            throw new ServiceException("通知不存在");
        }
        if (row.getIsRead() != null && row.getIsRead() == 1)
        {
            return;
        }
        row.setIsRead(1);
        row.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(row);
    }

    @Override
    @Transactional
    public void markReadAll(Long userId)
    {
        BlogUserNotification patch = new BlogUserNotification();
        patch.setIsRead(1);
        patch.setReadTime(LocalDateTime.now());
        LambdaQueryWrapper<BlogUserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogUserNotification::getUserId, userId).eq(BlogUserNotification::getIsRead, 0);
        notificationMapper.update(patch, wrapper);
    }

    @Override
    public BlogNotificationPreference getPreference(Long userId)
    {
        return getOrCreatePreference(userId);
    }

    @Override
    @Transactional
    public void updatePreference(Long userId, BlogNotificationPreference preference)
    {
        BlogNotificationPreference existing = getOrCreatePreference(userId);
        if (preference.getEnableInApp() != null)
        {
            existing.setEnableInApp(preference.getEnableInApp());
        }
        if (preference.getEnableEmail() != null)
        {
            existing.setEnableEmail(preference.getEnableEmail());
        }
        if (preference.getEnableComment() != null)
        {
            existing.setEnableComment(preference.getEnableComment());
        }
        if (preference.getEnableReply() != null)
        {
            existing.setEnableReply(preference.getEnableReply());
        }
        if (preference.getEnableSystem() != null)
        {
            existing.setEnableSystem(preference.getEnableSystem());
        }
        existing.setUpdateTime(LocalDateTime.now());
        preferenceMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void sendSystemNotification(SystemNotificationSendRequest request)
    {
        if (!notificationConfigService.enabled())
        {
            throw new ServiceException("通知功能已关闭");
        }
        List<Long> userIds = request.getUserIds();
        if (CollectionUtils.isEmpty(userIds))
        {
            SysUser query = new SysUser();
            query.setStatus("0");
            userIds = sysUserService.selectUserList(query).stream()
                    .map(SysUser::getUserId)
                    .filter(Objects::nonNull)
                    .toList();
        }
        String link = StringUtils.hasText(request.getLinkUrl()) ? request.getLinkUrl().trim() : null;
        for (Long userId : userIds)
        {
            dispatch(userId, BlogNotificationType.SYSTEM, request.getTitle().trim(),
                    request.getContent().trim(), link, null, null);
        }
    }

    private void notifyArticleComment(BlogComment comment)
    {
        BlogArticle article = articleMapper.selectById(comment.getArticleId());
        if (article == null || article.getAuthorUserId() == null)
        {
            return;
        }
        Long recipient = article.getAuthorUserId();
        if (Objects.equals(recipient, comment.getUserId()))
        {
            return;
        }
        String author = resolveDisplayName(comment);
        String title = author + " 评论了你的文章";
        String content = preview(comment.getContent());
        String link = buildArticleLink(article.getId(), comment.getId());
        dispatch(recipient, BlogNotificationType.COMMENT, title, content, link,
                BlogNotificationConstants.BIZ_COMMENT, comment.getId());
    }

    private void notifyCommentReply(BlogComment reply)
    {
        BlogComment parent = commentMapper.selectById(reply.getParentId());
        if (parent == null || parent.getUserId() == null)
        {
            return;
        }
        if (Objects.equals(parent.getUserId(), reply.getUserId()))
        {
            return;
        }
        String author = resolveDisplayName(reply);
        String title = author + " 回复了你的评论";
        String content = preview(reply.getContent());
        String link = buildArticleLink(reply.getArticleId(), reply.getId());
        dispatch(parent.getUserId(), BlogNotificationType.REPLY, title, content, link,
                BlogNotificationConstants.BIZ_COMMENT, reply.getId());
    }

    private void dispatch(Long recipientUserId, BlogNotificationType type, String title, String content,
            String linkUrl, String bizType, Long bizId)
    {
        if (recipientUserId == null)
        {
            return;
        }
        BlogNotificationPreference pref = getOrCreatePreference(recipientUserId);
        if (!flagOn(pref.getEnableInApp()))
        {
            return;
        }
        if (!typeEnabled(pref, type))
        {
            return;
        }
        if (bizId != null && isDuplicate(recipientUserId, type.name(), bizId))
        {
            return;
        }
        BlogUserNotification row = new BlogUserNotification();
        row.setUserId(recipientUserId);
        row.setType(type.name());
        row.setTitle(title);
        row.setContent(content);
        row.setLinkUrl(linkUrl);
        row.setBizType(bizType);
        row.setBizId(bizId);
        row.setIsRead(0);
        row.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(row);

        if (flagOn(pref.getEnableEmail()))
        {
            SysUser user = sysUserService.selectUserById(recipientUserId);
            if (user != null && StringUtils.hasText(user.getEmail()))
            {
                String html = buildEmailHtml(title, content, linkUrl);
                blogEmailService.sendNotificationEmailAsync(recipientUserId, user.getEmail(), title, html);
            }
        }
    }

    private boolean typeEnabled(BlogNotificationPreference pref, BlogNotificationType type)
    {
        return switch (type)
        {
            case COMMENT -> flagOn(pref.getEnableComment());
            case REPLY -> flagOn(pref.getEnableReply());
            case SYSTEM -> flagOn(pref.getEnableSystem());
        };
    }

    private boolean isDuplicate(Long userId, String type, Long bizId)
    {
        LocalDateTime since = LocalDateTime.now().minusMinutes(DEDUP_MINUTES);
        LambdaQueryWrapper<BlogUserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogUserNotification::getUserId, userId)
                .eq(BlogUserNotification::getType, type)
                .eq(BlogUserNotification::getBizId, bizId)
                .ge(BlogUserNotification::getCreateTime, since);
        return notificationMapper.selectCount(wrapper) > 0;
    }

    private BlogNotificationPreference getOrCreatePreference(Long userId)
    {
        BlogNotificationPreference pref = preferenceMapper.selectById(userId);
        if (pref != null)
        {
            return pref;
        }
        BlogNotificationPreference created = new BlogNotificationPreference();
        created.setUserId(userId);
        created.setEnableInApp(1);
        created.setEnableEmail(1);
        created.setEnableComment(1);
        created.setEnableReply(1);
        created.setEnableSystem(1);
        created.setUpdateTime(LocalDateTime.now());
        preferenceMapper.insert(created);
        return created;
    }

    private String resolveDisplayName(BlogComment comment)
    {
        if (comment.getUserId() != null)
        {
            SysUser user = sysUserService.selectUserById(comment.getUserId());
            if (user != null && StringUtils.hasText(user.getNickName()))
            {
                return user.getNickName();
            }
        }
        return StringUtils.hasText(comment.getGuestName()) ? comment.getGuestName() : "有人";
    }

    private String preview(String text)
    {
        if (!StringUtils.hasText(text))
        {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= CONTENT_PREVIEW_LEN
                ? trimmed
                : trimmed.substring(0, CONTENT_PREVIEW_LEN) + "…";
    }

    private String buildArticleLink(Long articleId, Long commentId)
    {
        return "/blog/" + articleId + "#comment-" + commentId;
    }

    private String buildEmailHtml(String title, String content, String linkUrl)
    {
        String base = notificationConfigService.publicBaseUrl();
        String href = StringUtils.hasText(linkUrl) ? base + linkUrl : base;
        return """
                <p>%s</p>
                <p>%s</p>
                <p><a href="%s">查看详情</a></p>
                """.formatted(escapeHtml(title), escapeHtml(content), href);
    }

    private String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private boolean flagOn(Integer flag)
    {
        return flag == null || flag == 1;
    }

    private NotificationVO toVo(BlogUserNotification row)
    {
        NotificationVO vo = new NotificationVO();
        vo.setId(row.getId());
        vo.setType(row.getType());
        vo.setTitle(row.getTitle());
        vo.setContent(row.getContent());
        vo.setLinkUrl(row.getLinkUrl());
        vo.setBizType(row.getBizType());
        vo.setBizId(row.getBizId());
        vo.setIsRead(row.getIsRead() != null && row.getIsRead() == 1);
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }
}
