package com.ruoyi.blog.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.ruoyi.blog.mapper.AiModuleConfigMapper;
import com.ruoyi.blog.mapper.AiProviderMapper;
import com.ruoyi.blog.service.impl.AiConfigServiceImpl;
import com.ruoyi.blog.service.impl.AiProviderServiceImpl;
import com.ruoyi.blog.service.llm.LlmClient;
import com.ruoyi.system.service.ISysConfigService;

import okhttp3.OkHttpClient;

class AiServiceDependencyGraphTest
{
    @Test
    void aiProviderAndConfigBeansCanBeCreatedTogetherWithoutCircularReference()
    {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
        {
            context.registerBean(ISysConfigService.class, () -> mock(ISysConfigService.class));
            context.registerBean(AiModuleConfigMapper.class, () -> mock(AiModuleConfigMapper.class));
            context.registerBean(AiProviderMapper.class, () -> mock(AiProviderMapper.class));
            context.registerBean(LlmClient.class, () -> mock(LlmClient.class));
            context.registerBean(OkHttpClient.class, () -> mock(OkHttpClient.class));
            context.register(AiProviderServiceImpl.class, AiConfigServiceImpl.class);

            assertDoesNotThrow(context::refresh);
        }
    }
}
