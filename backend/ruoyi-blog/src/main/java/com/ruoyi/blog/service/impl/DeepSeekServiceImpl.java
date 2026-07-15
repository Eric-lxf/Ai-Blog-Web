package com.ruoyi.blog.service.impl;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.service.AiPromptTemplateService;
import com.ruoyi.blog.service.AiProviderService;
import com.ruoyi.blog.service.DeepSeekService;
import com.ruoyi.blog.service.llm.LlmClient;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * 博客 AI 门面：从 {@link AiProviderService} 解析当前 Provider，再委托 {@link LlmClient}。
 * 类名保留 DeepSeek 以兼容既有注入点。
 */
@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService
{
    private final AiProviderService aiProviderService;

    private final AiPromptTemplateService aiPromptTemplateService;

    private final LlmClient llmClient;

    private final Executor aiTaskExecutor;

    @Autowired
    public DeepSeekServiceImpl(AiProviderService aiProviderService,
            AiPromptTemplateService aiPromptTemplateService,
            LlmClient llmClient,
            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor)
    {
        this.aiProviderService = aiProviderService;
        this.aiPromptTemplateService = aiPromptTemplateService;
        this.llmClient = llmClient;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @Override
    public String chatCompletion(AiCompletionRequest request)
    {
        AiProvider provider = requireProvider();
        AiPromptTemplate template = requireTemplate(
                StringUtils.hasText(request.getScene()) ? request.getScene() : "CHAT");
        OkHttpClient client = aiProviderService.httpClient(provider);
        BigDecimal effectiveTemperature = request.getTemperature() != null ? request.getTemperature() : template.getTemperature();
        return llmClient.chatCompletion(provider, request, template, provider.getDefaultModel(), effectiveTemperature, client);
    }

    @Override
    public void streamChat(AiChatRequest request, SseEmitter emitter)
    {
        AiProvider provider = aiProviderService.resolveActiveProvider();
        if (provider == null)
        {
            sendErrorAndComplete(emitter, "未配置 AI API Key，请在「AI模型配置」中添加，或设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        aiTaskExecutor.execute(() -> {
            try
            {
                AiPromptTemplate template = aiPromptTemplateService.getByScene(
                        StringUtils.hasText(request.getScene()) ? request.getScene() : "CHAT");
                if (template == null)
                {
                    sendErrorAndComplete(emitter, "未找到可用的 AI 提示词模板");
                    return;
                }
                OkHttpClient client = aiProviderService.httpClient(provider);
                llmClient.streamChat(provider, request, template, provider.getDefaultModel(), template.getTemperature(), client,
                        emitter);
                emitter.send("[DONE]");
                emitter.complete();
            }
            catch (ServiceException e)
            {
                sendErrorAndComplete(emitter, e.getMessage());
            }
            catch (Exception e)
            {
                log.error("AI stream error provider={}", provider.getName(), e);
                try
                {
                    sendErrorAndComplete(emitter, "AI 服务异常，请稍后重试");
                }
                catch (Exception ignored)
                {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    @Override
    public String recognizeImage(String imageUrl, String textPrompt)
    {
        AiProvider provider = requireProvider();
        OkHttpClient client = aiProviderService.httpClient(provider);
        return llmClient.recognizeImage(provider, imageUrl, textPrompt, provider.getVisionModel(), client);
    }

    private AiProvider requireProvider()
    {
        AiProvider provider = aiProviderService.resolveActiveProvider();
        if (provider == null)
        {
            throw new ServiceException("未配置 AI API Key，请在「AI模型配置」中添加，或设置环境变量 DEEPSEEK_API_KEY",
                    HttpStatus.ERROR);
        }
        return provider;
    }

    private AiPromptTemplate requireTemplate(String scene)
    {
        AiPromptTemplate template = aiPromptTemplateService.getByScene(scene);
        if (template == null)
        {
            throw new ServiceException("未找到可用的 AI 提示词模板", HttpStatus.ERROR);
        }
        return template;
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error").data(message));
        }
        catch (Exception e)
        {
            log.warn("Failed to send SSE error", e);
        }
        emitter.complete();
    }
}
