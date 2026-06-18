package com.ruoyi.wechat.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatMassPreviewRequest;
import com.ruoyi.wechat.dto.WechatMassSendRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatMassMessageService;
import com.ruoyi.wechat.vo.WechatMassRecordVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/mass")
public class WechatMassMessageController extends WechatControllerSupport
{
    private final WechatMassMessageService wechatMassMessageService;

    @PreAuthorize("@ss.hasPermi('wechat:mass:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatMassRecordVO> page = wechatMassMessageService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:mass:preview')")
    @PostMapping("/preview")
    public AjaxResult preview(@Valid @RequestBody WechatMassPreviewRequest request)
    {
        Map<String, Object> resp = wechatMassMessageService.preview(request);
        return AjaxResult.success(resp);
    }

    @PreAuthorize("@ss.hasPermi('wechat:mass:send')")
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody WechatMassSendRequest request)
    {
        return AjaxResult.success(wechatMassMessageService.send(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:mass:list')")
    @PostMapping("/{id}/sync-status")
    public AjaxResult syncStatus(@PathVariable Long id)
    {
        Map<String, Object> resp = wechatMassMessageService.syncStatus(id);
        return AjaxResult.success(resp);
    }
}
