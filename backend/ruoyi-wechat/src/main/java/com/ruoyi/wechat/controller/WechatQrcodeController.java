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
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatQrcodeCreateRequest;
import com.ruoyi.wechat.service.WechatQrcodeService;
import com.ruoyi.wechat.vo.WechatQrcodeVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/qrcode")
public class WechatQrcodeController extends WechatControllerSupport
{
    private final WechatQrcodeService wechatQrcodeService;

    @PreAuthorize("@ss.hasPermi('wechat:qrcode:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatQrcodeVO> page = wechatQrcodeService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:qrcode:add')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody WechatQrcodeCreateRequest request)
    {
        return AjaxResult.success(wechatQrcodeService.create(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:qrcode:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatQrcodeService.delete(id);
        return AjaxResult.success();
    }
}
