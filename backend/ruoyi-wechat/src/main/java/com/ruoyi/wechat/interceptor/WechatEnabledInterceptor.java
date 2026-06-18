package com.ruoyi.wechat.interceptor;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wechat.service.WechatConfigService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WechatEnabledInterceptor implements HandlerInterceptor
{
    private final WechatConfigService wechatConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (wechatConfigService.isWechatEnabled())
        {
            return true;
        }
        response.setStatus(HttpStatus.ERROR);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        AjaxResult body = AjaxResult.error(HttpStatus.ERROR, "微信公众号功能未启用，请在「模块配置」中开启");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
