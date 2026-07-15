package com.ruoyi.blog.service.llm;

import java.math.BigDecimal;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;

import okhttp3.OkHttpClient;

/**
 * 多厂商 LLM 调用抽象（OpenAI 兼容 / Anthropic Claude）。
 */
public interface LlmClient
{
    String chatCompletion(AiProvider provider, AiCompletionRequest request, AiPromptTemplate template, String textModel,
            BigDecimal effectiveTemperature, OkHttpClient client);

    void streamChat(AiProvider provider, AiChatRequest request, AiPromptTemplate template, String textModel,
            BigDecimal effectiveTemperature, OkHttpClient client, SseEmitter emitter) throws Exception;

    String recognizeImage(AiProvider provider, String imageUrl, String textPrompt, String visionModel, OkHttpClient client);

    void testConnection(AiProvider provider, OkHttpClient client);
}
