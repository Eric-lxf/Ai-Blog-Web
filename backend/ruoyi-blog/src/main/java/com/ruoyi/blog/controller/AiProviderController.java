package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.AiModuleConfigUpdateRequest;
import com.ruoyi.blog.dto.AiProviderPageQuery;
import com.ruoyi.blog.dto.AiProviderSaveRequest;
import com.ruoyi.blog.service.AiConfigService;
import com.ruoyi.blog.service.AiProviderService;
import com.ruoyi.blog.vo.AiProviderVO;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/blog/ai/provider")
public class AiProviderController extends BlogControllerSupport
{
    private final AiProviderService aiProviderService;
    private final AiConfigService aiConfigService;

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:list')")
    @GetMapping
    public TableDataInfo page(@Valid AiProviderPageQuery query)
    {
        Page<AiProviderVO> page = aiProviderService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:list') or @ss.hasPermi('blog:ai:chat') or @ss.hasPermi('blog:ai:write')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return AjaxResult.success(aiProviderService.listOptions());
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:query') or @ss.hasPermi('blog:ai:provider:list')")
    @GetMapping("/config")
    public AjaxResult moduleConfig()
    {
        return AjaxResult.success(aiConfigService.getModuleConfig());
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:edit')")
    @PostMapping("/config")
    public AjaxResult updateModuleConfig(@Valid @RequestBody AiModuleConfigUpdateRequest request)
    {
        aiConfigService.updateModuleConfig(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(aiProviderService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:add') or @ss.hasPermi('blog:ai:provider:edit')")
    @PostMapping
    public AjaxResult save(@Valid @RequestBody AiProviderSaveRequest request)
    {
        return AjaxResult.success(aiProviderService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        aiProviderService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('blog:ai:provider:test') or @ss.hasPermi('blog:ai:provider:query')")
    @PostMapping("/{id}/test")
    public AjaxResult test(@PathVariable Long id)
    {
        aiProviderService.testConnection(id);
        return AjaxResult.success("连接成功");
    }
}
