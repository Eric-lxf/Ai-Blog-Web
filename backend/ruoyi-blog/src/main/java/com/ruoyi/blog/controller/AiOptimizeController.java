package com.ruoyi.blog.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.dto.AiOptimizeRequest;
import com.ruoyi.blog.service.AiOptimizeService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/ai/optimize")
@RequiredArgsConstructor
public class AiOptimizeController extends BlogControllerSupport
{

    private final AiOptimizeService aiOptimizeService;

    @PreAuthorize("@ss.hasPermi('blog:ai:optimize')")
    @Log(title = "AI内容优化", businessType = BusinessType.AI, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    public AjaxResult optimize(@Valid @RequestBody AiOptimizeRequest request)
    {
        String content = aiOptimizeService.optimize(request);
        return AjaxResult.success(Map.of("content", content));
    }
}
