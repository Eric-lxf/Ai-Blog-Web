package com.ruoyi.wechat.controller;

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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.wechat.dto.WechatMenuSaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatMenuService;
import com.ruoyi.wechat.vo.WechatMenuVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/menu")
public class WechatMenuController extends WechatControllerSupport
{
    private final WechatMenuService wechatMenuService;

    @PreAuthorize("@ss.hasPermi('wechat:menu:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatMenuVO> page = wechatMenuService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:menu:query')")
    @GetMapping("/wechat/{accountId}")
    public AjaxResult getFromWechat(@PathVariable Long accountId)
    {
        return AjaxResult.success(wechatMenuService.getFromWechat(accountId));
    }

    @PreAuthorize("@ss.hasPermi('wechat:menu:add') or @ss.hasPermi('wechat:menu:edit')")
    @Log(title = "微信菜单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@Valid @RequestBody WechatMenuSaveRequest request)
    {
        return AjaxResult.success(wechatMenuService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:menu:publish')")
    @Log(title = "微信菜单", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable("id") Long id)
    {
        wechatMenuService.publish(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:menu:publish')")
    @Log(title = "微信菜单", businessType = BusinessType.DELETE)
    @DeleteMapping("/wechat/{accountId}")
    public AjaxResult deleteFromWechat(@PathVariable Long accountId)
    {
        wechatMenuService.deleteFromWechat(accountId);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:menu:sync')")
    @Log(title = "微信菜单", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/sync/{accountId}")
    public AjaxResult syncFromWechat(@PathVariable Long accountId)
    {
        return AjaxResult.success(wechatMenuService.syncFromWechat(accountId));
    }

    @PreAuthorize("@ss.hasPermi('wechat:menu:remove')")
    @Log(title = "微信菜单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatMenuService.delete(id);
        return AjaxResult.success();
    }
}
