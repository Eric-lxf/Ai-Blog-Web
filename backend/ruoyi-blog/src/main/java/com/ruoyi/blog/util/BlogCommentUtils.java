package com.ruoyi.blog.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.ruoyi.blog.domain.BlogComment;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;

public final class BlogCommentUtils
{
    private BlogCommentUtils()
    {
    }

    public static String guestKey(String ip, String userAgent)
    {
        String raw = StringUtils.defaultString(ip) + "|" + StringUtils.defaultString(userAgent);
        return sha256(raw).substring(0, 32);
    }

    public static String currentGuestKey()
    {
        return guestKey(IpUtils.getIpAddr(), userAgent());
    }

    public static String userAgent()
    {
        try
        {
            var request = com.ruoyi.common.utils.ServletUtils.getRequest();
            return request == null ? "" : StringUtils.defaultString(request.getHeader("User-Agent"));
        }
        catch (Exception ex)
        {
            return "";
        }
    }

    public static LoginUser loginUserOrNull()
    {
        try
        {
            return com.ruoyi.common.utils.SecurityUtils.getLoginUser();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public static double calcSortScore(BlogComment comment)
    {
        int likes = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
        int replies = comment.getReplyCount() == null ? 0 : comment.getReplyCount();
        LocalDateTime time = comment.getCreateTime() == null ? LocalDateTime.now() : comment.getCreateTime();
        long epoch = time.atZone(ZoneId.systemDefault()).toEpochSecond();
        return likes * 3.0 + replies * 2.0 + epoch / 45000.0;
    }

    private static String sha256(String raw)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
            {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException ex)
        {
            return Integer.toHexString(raw.hashCode());
        }
    }
}
