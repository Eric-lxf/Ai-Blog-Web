package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wechat.dto.WechatModuleConfigUpdateRequest;
import com.ruoyi.wechat.service.WechatConfigService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/config")
public class WechatConfigController extends WechatControllerSupport
{
    private final WechatConfigService wechatConfigService;

    @PreAuthorize("@ss.hasPermi('wechat:config:query')")
    @GetMapping
    public AjaxResult get()
    {
        return AjaxResult.success(wechatConfigService.getModuleConfig());
    }

    @PreAuthorize("@ss.hasPermi('wechat:config:edit')")
    @PostMapping
    public AjaxResult update(@Valid @RequestBody WechatModuleConfigUpdateRequest request)
    {
        wechatConfigService.updateModuleConfig(request);
        return AjaxResult.success();
    }
}
