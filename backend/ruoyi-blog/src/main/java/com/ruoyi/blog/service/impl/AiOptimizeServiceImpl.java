package com.ruoyi.blog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.dto.AiOptimizeRequest;
import com.ruoyi.blog.service.AiOptimizeService;
import com.ruoyi.blog.service.DeepSeekService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiOptimizeServiceImpl implements AiOptimizeService
{

    private final DeepSeekService deepSeekService;

    @Override
    public String optimize(AiOptimizeRequest request)
    {
        AiCompletionRequest completion = new AiCompletionRequest();
        completion.setScene(StringUtils.hasText(request.getScene()) ? request.getScene() : "REWRITE");
        completion.setPrompt(request.getContent());
        completion.setCustomSystemPrompt(request.getCustomSystemPrompt());
        completion.setTemperature(request.getTemperature());
        return deepSeekService.chatCompletion(completion).trim();
    }
}
