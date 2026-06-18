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
import com.ruoyi.wechat.dto.WechatDraftBatchRequest;
import com.ruoyi.wechat.dto.WechatDraftDeleteRequest;
import com.ruoyi.wechat.dto.WechatDraftGetRequest;
import com.ruoyi.wechat.dto.WechatDraftSaveRequest;
import com.ruoyi.wechat.dto.WechatDraftUpdateRequest;
import com.ruoyi.wechat.service.WechatDraftService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/draft")
public class WechatDraftController extends WechatControllerSupport
{
    private final WechatDraftService wechatDraftService;

    @PreAuthorize("@ss.hasPermi('wechat:draft:list')")
    @PostMapping("/batchget")
    public AjaxResult batchGet(@Valid @RequestBody WechatDraftBatchRequest request)
    {
        return AjaxResult.success(wechatDraftService.batchGet(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:draft:list')")
    @PostMapping("/get")
    public AjaxResult get(@Valid @RequestBody WechatDraftGetRequest request)
    {
        return AjaxResult.success(wechatDraftService.get(request.getAccountId(), request.getMediaId()));
    }

    @PreAuthorize("@ss.hasPermi('wechat:draft:list')")
    @GetMapping("/count")
    public AjaxResult count(@RequestParam Long accountId)
    {
        return AjaxResult.success(wechatDraftService.count(accountId));
    }

    @PreAuthorize("@ss.hasPermi('wechat:draft:add')")
    @PostMapping
    public AjaxResult add(@Valid @RequestBody WechatDraftSaveRequest request)
    {
        return AjaxResult.success(wechatDraftService.add(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:draft:edit')")
    @PostMapping("/update")
    public AjaxResult update(@Valid @RequestBody WechatDraftUpdateRequest request)
    {
        wechatDraftService.update(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:draft:remove')")
    @PostMapping("/delete")
    public AjaxResult delete(@Valid @RequestBody WechatDraftDeleteRequest request)
    {
        wechatDraftService.delete(request.getAccountId(), request.getMediaId());
        return AjaxResult.success();
    }
}
