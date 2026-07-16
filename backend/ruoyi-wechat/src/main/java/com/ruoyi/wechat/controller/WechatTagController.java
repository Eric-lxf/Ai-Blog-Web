package com.ruoyi.wechat.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatTagMarkRequest;
import com.ruoyi.wechat.dto.WechatTagSaveRequest;
import com.ruoyi.wechat.service.WechatTagService;
import com.ruoyi.wechat.vo.WechatTagSyncResultVO;
import com.ruoyi.wechat.vo.WechatTagVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/tag")
public class WechatTagController extends WechatControllerSupport
{
    private final WechatTagService wechatTagService;

    @PreAuthorize("@ss.hasPermi('wechat:tag:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatTagVO> page = wechatTagService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:tag:list')")
    @GetMapping("/options")
    public AjaxResult options(@RequestParam Long accountId)
    {
        List<WechatTagVO> list = wechatTagService.listByAccount(accountId);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('wechat:tag:add') or @ss.hasPermi('wechat:tag:edit')")
    @Log(title = "微信标签", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@Valid @RequestBody WechatTagSaveRequest request)
    {
        return AjaxResult.success(wechatTagService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:tag:remove')")
    @Log(title = "微信标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatTagService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:tag:sync')")
    @Log(title = "微信标签", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/sync")
    public AjaxResult sync(@RequestParam Long accountId)
    {
        WechatTagSyncResultVO result = wechatTagService.syncFromWechat(accountId);
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('wechat:tag:mark')")
    @Log(title = "微信标签", businessType = BusinessType.UPDATE)
    @PostMapping("/mark")
    public AjaxResult mark(@Valid @RequestBody WechatTagMarkRequest request)
    {
        wechatTagService.batchMark(request);
        return AjaxResult.success();
    }
}
