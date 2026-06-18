package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WechatQrcodeUtilsTest
{
    @Test
    void buildImageUrl_should_encode_ticket()
    {
        String url = WechatQrcodeUtils.buildImageUrl("ticket+value");
        assertTrue(url.contains("showqrcode"));
        assertTrue(url.contains("ticket"));
    }

    @Test
    void parseSceneFromSubscribeEventKey_should_strip_prefix()
    {
        assertEquals("123", WechatQrcodeUtils.parseSceneFromSubscribeEventKey("qrscene_123"));
        assertEquals("channel_a", WechatQrcodeUtils.parseSceneFromSubscribeEventKey("qrscene_channel_a"));
    }
}
