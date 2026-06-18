package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WechatContentConverterTest
{
    @Mock
    private WechatMediaUploadService wechatMediaUploadService;

    @InjectMocks
    private WechatContentConverter converter;

    @Test
    void toWechatHtml_should_remove_dangerous_tags()
    {
        String result = converter.toWechatHtml("<script>alert(1)</script><p>Hello</p>", null);
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    void toWechatHtml_should_render_markdown()
    {
        String result = converter.toWechatHtml("# Title\n\nHello **world**", null);
        assertTrue(result.contains("Title"));
        assertTrue(result.contains("world"));
    }

    @Test
    void toDigest_should_truncate()
    {
        String source = "<p>" + "a".repeat(150) + "</p>";
        String digest = converter.toDigest(source);
        assertTrue(digest.length() <= 120);
    }
}
