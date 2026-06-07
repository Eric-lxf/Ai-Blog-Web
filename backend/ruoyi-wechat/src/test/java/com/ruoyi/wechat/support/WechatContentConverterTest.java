package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WechatContentConverterTest
{
    private final WechatContentConverter converter = new WechatContentConverter();

    @Test
    void toWechatHtml_should_remove_dangerous_tags()
    {
        String result = converter.toWechatHtml("<script>alert(1)</script><p>Hello</p>");
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    void toDigest_should_truncate()
    {
        String source = "<p>" + "a".repeat(150) + "</p>";
        String digest = converter.toDigest(source);
        assertTrue(digest.length() <= 120);
    }
}
