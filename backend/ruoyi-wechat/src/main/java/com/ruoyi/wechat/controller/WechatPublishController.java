package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatPushRequest;
import com.ruoyi.wechat.service.WechatPublishService;
import com.ruoyi.wechat.service.WechatPushOrchestrator;
import com.ruoyi.wechat.vo.WechatPublishRecordVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WechatPublishController extends WechatControllerSupport
{
    private final WechatPublishService wechatPublishService;
    private final WechatPushOrchestrator wechatPushOrchestrator;

    @PreAuthorize("@ss.hasPermi('wechat:publish:list')")
    @GetMapping("/wechat/publish")
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatPublishRecordVO> page = wechatPublishService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:push')")
    @PostMapping("/wechat/push")
    public AjaxResult push(@Valid @RequestBody WechatPushRequest request)
    {
        Long recordId = wechatPushOrchestrator.push(request);
        return AjaxResult.success(recordId);
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:push')")
    @PostMapping("/wechat/publish/push")
    public AjaxResult pushViaPublish(@Valid @RequestBody WechatPushRequest request)
    {
        Long recordId = wechatPushOrchestrator.push(request);
        return AjaxResult.success(recordId);
    }
}
