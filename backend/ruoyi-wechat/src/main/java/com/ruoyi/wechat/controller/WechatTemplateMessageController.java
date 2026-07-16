package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.wechat.dto.WechatTemplateSendRequest;
import com.ruoyi.wechat.service.WechatTemplateMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/template")
public class WechatTemplateMessageController extends WechatControllerSupport
{
    private final WechatTemplateMessageService wechatTemplateMessageService;

    @PreAuthorize("@ss.hasPermi('wechat:template:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long accountId)
    {
        return AjaxResult.success(wechatTemplateMessageService.listTemplates(accountId));
    }

    @PreAuthorize("@ss.hasPermi('wechat:template:send')")
    @Log(title = "微信模板消息", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody WechatTemplateSendRequest request)
    {
        wechatTemplateMessageService.send(request);
        return AjaxResult.success();
    }
}
