package com.ruoyi.blog.service.impl;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ruoyi.blog.constant.BlogAnalyticsConstants;
import com.ruoyi.blog.domain.BlogVisitLog;
import com.ruoyi.blog.mapper.BlogVisitLogMapper;
import com.ruoyi.blog.service.AnalyticsConfigService;
import com.ruoyi.blog.service.BlogVisitService;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.sign.Md5Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogVisitServiceImpl implements BlogVisitService
{
    private final BlogVisitLogMapper visitLogMapper;
    private final AnalyticsConfigService analyticsConfigService;

    @Override
    public void recordFromRequest(String pageType, Long articleId)
    {
        HttpServletRequest request = ServletUtils.getRequest();
        String visitorId = request != null ? request.getHeader(BlogAnalyticsConstants.VISITOR_HEADER) : null;
        record(pageType, articleId, visitorId);
    }

    @Override
    @Async("aiTaskExecutor")
    public void record(String pageType, Long articleId, String visitorId)
    {
        if (!analyticsConfigService.enabled())
        {
            return;
        }
        try
        {
            HttpServletRequest request = ServletUtils.getRequest();
            String ip = request != null ? IpUtils.getIpAddr(request) : "";
            String userAgent = request != null ? request.getHeader("User-Agent") : "";
            String referer = request != null ? request.getHeader("Referer") : null;
            String visitorKey = resolveVisitorKey(visitorId, ip, userAgent);
            Long userId = resolveUserId();

            BlogVisitLog row = new BlogVisitLog();
            row.setPageType(pageType);
            row.setArticleId(articleId);
            row.setVisitorKey(visitorKey);
            row.setUserId(userId);
            row.setIp(ip);
            row.setUserAgent(StringUtils.hasText(userAgent) && userAgent.length() > 500
                    ? userAgent.substring(0, 500) : userAgent);
            row.setReferer(StringUtils.hasText(referer) && referer.length() > 500
                    ? referer.substring(0, 500) : referer);
            row.setRefererHost(parseRefererHost(referer));
            row.setRegion(resolveRegion(ip));
            row.setCreateTime(LocalDateTime.now());
            visitLogMapper.insert(row);
        }
        catch (Exception ex)
        {
            log.debug("Record blog visit failed: {}", ex.getMessage());
        }
    }

    private Long resolveUserId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private String resolveVisitorKey(String visitorId, String ip, String userAgent)
    {
        if (StringUtils.hasText(visitorId))
        {
            return visitorId.trim().length() > 64 ? visitorId.trim().substring(0, 64) : visitorId.trim();
        }
        String raw = (ip == null ? "" : ip) + "|" + (userAgent == null ? "" : userAgent);
        return Md5Utils.hash(raw);
    }

    private String parseRefererHost(String referer)
    {
        if (!StringUtils.hasText(referer))
        {
            return BlogAnalyticsConstants.REFERER_DIRECT;
        }
        try
        {
            URI uri = new URI(referer.trim());
            String host = uri.getHost();
            if (!StringUtils.hasText(host))
            {
                return BlogAnalyticsConstants.REFERER_OTHER;
            }
            return host.toLowerCase();
        }
        catch (Exception ex)
        {
            return BlogAnalyticsConstants.REFERER_OTHER;
        }
    }

    private String resolveRegion(String ip)
    {
        if (!StringUtils.hasText(ip))
        {
            return "未知";
        }
        try
        {
            String location = AddressUtils.getRealAddressByIP(ip);
            return StringUtils.hasText(location) ? location : "未知";
        }
        catch (Exception ex)
        {
            return "未知";
        }
    }
}
