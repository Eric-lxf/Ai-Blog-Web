package com.ruoyi.wechat.support;

import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

/**
 * 将存储的菜单 JSON 解析为微信 {@code /cgi-bin/menu/create} 所需请求体。
 * <p>
 * 支持两种存储格式：
 * <ul>
 *   <li>按钮数组：{@code [{"type":"click","name":"...","key":"..."}]}</li>
 *   <li>完整对象：{@code {"button":[...]}}</li>
 * </ul>
 */
public final class WechatMenuPayloadParser
{
    private WechatMenuPayloadParser()
    {
    }

    public static Map<String, Object> parseCreatePayload(String menuJson, ObjectMapper objectMapper)
    {
        if (!StringUtils.hasText(menuJson))
        {
            throw new ServiceException("menu json is empty", HttpStatus.BAD_REQUEST);
        }
        try
        {
            String trimmed = menuJson.trim();
            if (trimmed.startsWith("["))
            {
                List<Object> buttons = objectMapper.readValue(trimmed, new TypeReference<List<Object>>()
                {
                });
                return Map.of("button", buttons);
            }
            Map<String, Object> root = objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>()
            {
            });
            Object button = root.get("button");
            if (button == null)
            {
                throw new ServiceException("menu json must contain a button array", HttpStatus.BAD_REQUEST);
            }
            if (button instanceof String)
            {
                throw new ServiceException("button must be a json array, not a string", HttpStatus.BAD_REQUEST);
            }
            return Map.of("button", button);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("invalid menu json: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 从微信 {@code /cgi-bin/menu/get} 响应中提取按钮数组，序列化为本地存储格式（按钮 JSON 数组字符串）。
     */
    public static String toStoredJson(Map<String, Object> wechatMenuResponse, ObjectMapper objectMapper)
    {
        Object menuObj = wechatMenuResponse.get("menu");
        if (!(menuObj instanceof Map<?, ?> menuMap))
        {
            throw new ServiceException("wechat menu not found", HttpStatus.BAD_REQUEST);
        }
        Object button = menuMap.get("button");
        if (button == null)
        {
            throw new ServiceException("wechat menu button array is empty", HttpStatus.BAD_REQUEST);
        }
        try
        {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(button);
        }
        catch (Exception e)
        {
            throw new ServiceException("failed to serialize wechat menu: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
