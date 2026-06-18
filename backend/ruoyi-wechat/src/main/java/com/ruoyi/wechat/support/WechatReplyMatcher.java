package com.ruoyi.wechat.support;

import java.util.Comparator;
import java.util.List;

import org.springframework.util.StringUtils;

import com.ruoyi.wechat.domain.WechatAutoReply;

public final class WechatReplyMatcher
{
    private WechatReplyMatcher()
    {
    }

    public static String resolve(List<WechatAutoReply> rules, String msgType, String event, String eventKey,
            String content)
    {
        if (rules == null || rules.isEmpty())
        {
            return "";
        }
        if ("event".equalsIgnoreCase(msgType))
        {
            if ("subscribe".equalsIgnoreCase(event))
            {
                String scene = WechatQrcodeUtils.parseSceneFromSubscribeEventKey(eventKey);
                if (StringUtils.hasText(scene))
                {
                    String scanReply = matchScanReply(rules, scene);
                    if (StringUtils.hasText(scanReply))
                    {
                        return scanReply;
                    }
                }
                return loadReplyContent(rules, "subscribe");
            }
            if ("SCAN".equalsIgnoreCase(event))
            {
                return matchScanReply(rules, eventKey);
            }
            return "";
        }
        if ("text".equalsIgnoreCase(msgType) && StringUtils.hasText(content))
        {
            String keywordReply = matchKeywordReply(rules, content);
            if (StringUtils.hasText(keywordReply))
            {
                return keywordReply;
            }
            return loadReplyContent(rules, "default");
        }
        return "";
    }

    private static String loadReplyContent(List<WechatAutoReply> rules, String replyType)
    {
        return rules.stream().filter(rule -> rule.getEnabled() != null && rule.getEnabled() == 1)
                .filter(rule -> replyType.equals(rule.getReplyType()))
                .max(Comparator.comparing(WechatAutoReply::getUpdateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(WechatAutoReply::getContent).filter(StringUtils::hasText).orElse("");
    }

    private static String matchScanReply(List<WechatAutoReply> rules, String scene)
    {
        if (!StringUtils.hasText(scene))
        {
            return "";
        }
        return rules.stream().filter(rule -> rule.getEnabled() != null && rule.getEnabled() == 1)
                .filter(rule -> "scan".equals(rule.getReplyType()))
                .filter(rule -> StringUtils.hasText(rule.getKeyword()))
                .sorted(Comparator.comparing(WechatAutoReply::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(rule -> scene.equals(rule.getKeyword().trim()))
                .map(WechatAutoReply::getContent).filter(StringUtils::hasText).findFirst().orElse("");
    }

    private static String matchKeywordReply(List<WechatAutoReply> rules, String content)
    {
        return rules.stream().filter(rule -> rule.getEnabled() != null && rule.getEnabled() == 1)
                .filter(rule -> "keyword".equals(rule.getReplyType()))
                .filter(rule -> StringUtils.hasText(rule.getKeyword()))
                .sorted(Comparator.comparing(WechatAutoReply::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(rule -> matchesKeyword(rule, content)).map(WechatAutoReply::getContent)
                .filter(StringUtils::hasText).findFirst().orElse("");
    }

    private static boolean matchesKeyword(WechatAutoReply rule, String content)
    {
        if (rule.getMatchType() != null && rule.getMatchType() == 2)
        {
            return content.equals(rule.getKeyword());
        }
        return content.contains(rule.getKeyword());
    }
}
