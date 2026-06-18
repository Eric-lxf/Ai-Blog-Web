package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wechat.dto.WechatKefuSendRequest;
import com.ruoyi.wechat.service.WechatKefuService;
import com.ruoyi.wechat.vo.WechatKefuSessionVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/kefu")
public class WechatKefuController extends WechatControllerSupport
{
    private final WechatKefuService wechatKefuService;

    @PreAuthorize("@ss.hasPermi('wechat:kefu:send')")
    @GetMapping("/session")
    public AjaxResult session(@RequestParam Long accountId, @RequestParam String openId)
    {
        WechatKefuSessionVO vo = wechatKefuService.checkSession(accountId, openId);
        return AjaxResult.success(vo);
    }

    @PreAuthorize("@ss.hasPermi('wechat:kefu:send')")
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody WechatKefuSendRequest request)
    {
        wechatKefuService.sendText(request);
        return AjaxResult.success();
    }
}
