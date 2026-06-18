package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;

class WechatMenuPayloadParserTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseCreatePayload_should_accept_button_array()
    {
        String json = "[{\"type\":\"click\",\"name\":\"今日歌曲\",\"key\":\"V1001_TODAY_MUSIC\"}]";
        Map<String, Object> payload = WechatMenuPayloadParser.parseCreatePayload(json, objectMapper);
        assertTrue(payload.containsKey("button"));
        List<?> buttons = (List<?>) payload.get("button");
        assertEquals(1, buttons.size());
    }

    @Test
    void parseCreatePayload_should_accept_full_object()
    {
        String json = "{\"button\":[{\"type\":\"view\",\"name\":\"官网\",\"url\":\"https://example.com\"}]}";
        Map<String, Object> payload = WechatMenuPayloadParser.parseCreatePayload(json, objectMapper);
        List<?> buttons = (List<?>) payload.get("button");
        assertEquals(1, buttons.size());
    }

    @Test
    void parseCreatePayload_should_reject_missing_button()
    {
        assertThrows(ServiceException.class,
                () -> WechatMenuPayloadParser.parseCreatePayload("{\"name\":\"bad\"}", objectMapper));
    }
}
