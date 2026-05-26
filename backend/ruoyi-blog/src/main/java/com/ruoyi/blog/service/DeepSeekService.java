package com.ruoyi.blog.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;

public interface DeepSeekService
{

    void streamChat(AiChatRequest request, SseEmitter emitter);

    String chatCompletion(AiCompletionRequest request);
}
