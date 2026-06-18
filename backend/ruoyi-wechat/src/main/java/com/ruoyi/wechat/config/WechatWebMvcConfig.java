package com.ruoyi.wechat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.wechat.interceptor.WechatEnabledInterceptor;
import com.ruoyi.wechat.service.WechatConfigService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WechatWebMvcConfig implements WebMvcConfigurer
{
    private final WechatConfigService wechatConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(new WechatEnabledInterceptor(wechatConfigService, objectMapper))
                .addPathPatterns("/wechat/**")
                .excludePathPatterns("/wechat/config", "/wechat/config/**");
    }
}
