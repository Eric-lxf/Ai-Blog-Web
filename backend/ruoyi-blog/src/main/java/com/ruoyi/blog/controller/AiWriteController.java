package com.ruoyi.blog.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.dto.AiWriteWizardRequest;
import com.ruoyi.blog.dto.OutlineNodeDTO;
import com.ruoyi.blog.service.AiWriteService;
import com.ruoyi.common.core.domain.AjaxResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/ai/write")
@RequiredArgsConstructor
@PreAuthorize("@ss.hasPermi('blog:ai:write')")
public class AiWriteController extends BlogControllerSupport
{

    private final AiWriteService aiWriteService;

    @PostMapping("/titles")
    public AjaxResult titles(@Valid @RequestBody AiWriteWizardRequest request)
    {
        List<String> titles = aiWriteService.generateTitles(request);
        return AjaxResult.success(titles);
    }

    @PostMapping("/summary")
    public AjaxResult summary(@Valid @RequestBody AiWriteWizardRequest request)
    {
        // 必须显式传入 msg，否则 String 会匹配 success(String msg) 而非 success(Object data)
        return AjaxResult.success("操作成功", aiWriteService.generateSummary(request));
    }

    @PostMapping("/outline")
    public AjaxResult outline(@Valid @RequestBody AiWriteWizardRequest request)
    {
        List<OutlineNodeDTO> outline = aiWriteService.generateOutline(request);
        return AjaxResult.success(outline);
    }

    @PostMapping("/generate")
    public AjaxResult generate(@Valid @RequestBody AiWriteWizardRequest request)
    {
        Long taskId = aiWriteService.submitGenerateArticle(request);
        return AjaxResult.success(Map.of("taskId", taskId));
    }
}
