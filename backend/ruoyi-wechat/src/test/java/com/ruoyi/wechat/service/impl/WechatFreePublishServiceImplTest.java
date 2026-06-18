package com.ruoyi.wechat.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatTokenService;

@ExtendWith(MockitoExtension.class)
class WechatFreePublishServiceImplTest
{
    @Mock
    private WechatTokenService wechatTokenService;

    @Mock
    private WechatApiClient wechatApiClient;

    @InjectMocks
    private WechatFreePublishServiceImpl service;

    @Test
    void submit_should_call_freepublish_submit()
    {
        when(wechatTokenService.getAccessToken(1L)).thenReturn("token");
        when(wechatApiClient.postJson(contains("/cgi-bin/freepublish/submit"), any()))
                .thenReturn(Map.of("errcode", 0, "publish_id", "10001"));

        Map<String, Object> resp = service.submit(1L, "MEDIA_ID");

        assertEquals("10001", resp.get("publish_id"));
        verify(wechatApiClient).postJson(contains("/cgi-bin/freepublish/submit"), any());
    }
}
