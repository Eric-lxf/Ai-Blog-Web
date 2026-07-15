package com.ruoyi.blog.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;

public interface DeepSeekService
{

    void streamChat(AiChatRequest request, SseEmitter emitter, String moduleCode);

    String chatCompletion(AiCompletionRequest request, String moduleCode);

    /**
     * 使用视觉模型识别图片内容，返回模型原始文本输出。
     * 需要配置 {@code deepseek.vision-model} 并确保 API Key 有对应权限。
     */
    String recognizeImage(String imageUrl, String textPrompt, String moduleCode);
}
