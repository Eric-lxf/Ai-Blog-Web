package com.ruoyi.blog.service.impl;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.service.AiPromptTemplateService;
import com.ruoyi.blog.service.AiProviderService;
import com.ruoyi.blog.service.AiResolvedModelConfig;
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
    private static final String NO_PROVIDER_MESSAGE = "未配置可用 AI Provider，请在「AI模型配置」中添加并启用 Provider";

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
    public String chatCompletion(AiCompletionRequest request, String moduleCode)
    {
        AiResolvedModelConfig resolved = requireResolvedConfig(moduleCode);
        AiPromptTemplate template = requireTemplate(StringUtils.hasText(request.getScene()) ? request.getScene() : "CHAT");
        OkHttpClient client = aiProviderService.httpClient(resolved.getProvider());
        BigDecimal effectiveTemperature = resolveEffectiveTemperature(
                request.getTemperature(), resolved.getTemperatureOverride(), template.getTemperature());
        return llmClient.chatCompletion(
                resolved.getProvider(), request, template, resolved.getTextModel(), effectiveTemperature, client);
    }

    @Override
    public void streamChat(AiChatRequest request, SseEmitter emitter, String moduleCode)
    {
        AiResolvedModelConfig resolved;
        try
        {
            resolved = requireResolvedConfig(moduleCode);
        }
        catch (ServiceException e)
        {
            sendErrorAndComplete(emitter, e.getMessage());
            return;
        }

        aiTaskExecutor.execute(() -> {
            try
            {
                AiPromptTemplate template = requireTemplate(
                        StringUtils.hasText(request.getScene()) ? request.getScene() : "CHAT");
                OkHttpClient client = aiProviderService.httpClient(resolved.getProvider());
                BigDecimal effectiveTemperature = resolveEffectiveTemperature(
                        null, resolved.getTemperatureOverride(), template.getTemperature());
                llmClient.streamChat(
                        resolved.getProvider(), request, template, resolved.getTextModel(), effectiveTemperature, client,
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
                log.error("AI stream error provider={}", resolved.getProvider().getName(), e);
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
    public String recognizeImage(String imageUrl, String textPrompt, String moduleCode)
    {
        AiResolvedModelConfig resolved = requireResolvedConfig(moduleCode);
        OkHttpClient client = aiProviderService.httpClient(resolved.getProvider());
        log.info("AI recognize module={} providerId={} provider={} visionModel={} textModel={} source={}",
                moduleCode,
                resolved.getProvider().getId(),
                resolved.getProvider().getName(),
                resolved.getVisionModel(),
                resolved.getTextModel(),
                resolved.getSource());
        return llmClient.recognizeImage(resolved.getProvider(), imageUrl, textPrompt, resolved.getVisionModel(), client);
    }

    private AiResolvedModelConfig requireResolvedConfig(String moduleCode)
    {
        try
        {
            return aiProviderService.resolveForModule(moduleCode);
        }
        catch (ServiceException e)
        {
            if ("未配置可用 AI Provider".equals(e.getMessage()))
            {
                throw new ServiceException(NO_PROVIDER_MESSAGE, HttpStatus.ERROR);
            }
            throw e;
        }
    }

    private AiPromptTemplate requireTemplate(String scene)
    {
        AiPromptTemplate template = aiPromptTemplateService.getByScene(scene);
        if (template == null || isBillAdviceFallback(scene, template))
        {
            throw new ServiceException("未找到可用的 AI 提示词模板", HttpStatus.ERROR);
        }
        return template;
    }

    private boolean isBillAdviceFallback(String scene, AiPromptTemplate template)
    {
        return "BILL_ADVICE".equals(scene) && !scene.equals(template.getSceneType());
    }

    private BigDecimal resolveEffectiveTemperature(
            BigDecimal requestTemperature, BigDecimal moduleTemperature, BigDecimal templateTemperature)
    {
        if (requestTemperature != null)
        {
            return requestTemperature;
        }
        if (moduleTemperature != null)
        {
            return moduleTemperature;
        }
        return templateTemperature;
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
