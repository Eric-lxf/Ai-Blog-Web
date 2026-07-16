package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatFansService;
import com.ruoyi.wechat.vo.WechatFansSyncResultVO;
import com.ruoyi.wechat.vo.WechatFansVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/fans")
public class WechatFansController extends WechatControllerSupport
{
    private final WechatFansService wechatFansService;

    @PreAuthorize("@ss.hasPermi('wechat:fans:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatFansVO> page = wechatFansService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:fans:list')")
    @Log(title = "微信粉丝", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/sync")
    public AjaxResult sync(@RequestParam("accountId") Long accountId)
    {
        WechatFansSyncResultVO result = wechatFansService.syncFromWechat(accountId);
        return AjaxResult.success(result);
    }
}
