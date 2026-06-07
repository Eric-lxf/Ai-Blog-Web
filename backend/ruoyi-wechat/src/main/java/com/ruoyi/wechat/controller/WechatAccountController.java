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
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatAccountSaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatAccountService;
import com.ruoyi.wechat.vo.WechatAccountVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/account")
public class WechatAccountController extends WechatControllerSupport
{
    private final WechatAccountService wechatAccountService;

    @PreAuthorize("@ss.hasPermi('wechat:account:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatAccountVO> page = wechatAccountService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:account:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(wechatAccountService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('wechat:account:add') or @ss.hasPermi('wechat:account:edit')")
    @PostMapping
    public AjaxResult save(@Valid @RequestBody WechatAccountSaveRequest request)
    {
        return AjaxResult.success(wechatAccountService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:account:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatAccountService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:account:query')")
    @PostMapping("/{id}/test")
    public AjaxResult test(@PathVariable Long id)
    {
        wechatAccountService.testConnection(id);
        return AjaxResult.success("ok");
    }
}
