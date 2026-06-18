package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ruoyi.wechat.domain.WechatAutoReply;

class WechatReplyMatcherTest
{
    @Test
    void resolve_should_return_subscribe_reply()
    {
        WechatAutoReply subscribe = rule("subscribe", null, "welcome", 1, 1);
        assertEquals("welcome", WechatReplyMatcher.resolve(List.of(subscribe), "event", "subscribe", "", ""));
    }

    @Test
    void resolve_should_return_scan_reply_for_scan_event()
    {
        WechatAutoReply scan = rule("scan", "channel_a", "scan-welcome", 1, 1);
        assertEquals("scan-welcome", WechatReplyMatcher.resolve(List.of(scan), "event", "SCAN", "channel_a", ""));
    }

    @Test
    void resolve_should_return_scan_reply_for_qr_subscribe()
    {
        WechatAutoReply scan = rule("scan", "1001", "channel-welcome", 1, 1);
        WechatAutoReply subscribe = rule("subscribe", null, "generic-welcome", 1, 2);
        assertEquals("channel-welcome",
                WechatReplyMatcher.resolve(List.of(scan, subscribe), "event", "subscribe", "qrscene_1001", ""));
    }

    @Test
    void resolve_should_return_keyword_reply_before_default()
    {
        WechatAutoReply keyword = rule("keyword", "hello", "keyword-reply", 1, 1);
        WechatAutoReply defaultRule = rule("default", null, "default-reply", 1, 2);
        assertEquals("keyword-reply",
                WechatReplyMatcher.resolve(List.of(keyword, defaultRule), "text", "", "", "say hello"));
    }

    @Test
    void resolve_should_fallback_to_default_reply()
    {
        WechatAutoReply defaultRule = rule("default", null, "default-reply", 1, 1);
        assertEquals("default-reply", WechatReplyMatcher.resolve(List.of(defaultRule), "text", "", "", "no match"));
    }

    @Test
    void resolve_should_return_empty_for_unsupported_event()
    {
        WechatAutoReply subscribe = rule("subscribe", null, "welcome", 1, 1);
        assertEquals("", WechatReplyMatcher.resolve(List.of(subscribe), "event", "unsubscribe", "", ""));
    }

    private WechatAutoReply rule(String type, String keyword, String content, int enabled, int order)
    {
        WechatAutoReply reply = new WechatAutoReply();
        reply.setReplyType(type);
        reply.setKeyword(keyword);
        reply.setContent(content);
        reply.setEnabled(enabled);
        reply.setMatchType(1);
        reply.setUpdateTime(LocalDateTime.of(2026, 1, order, 0, 0));
        return reply;
    }
}
