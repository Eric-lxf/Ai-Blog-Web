package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatMaterialService;
import com.ruoyi.wechat.vo.WechatMaterialVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/material")
public class WechatMaterialController extends WechatControllerSupport
{
    private final WechatMaterialService wechatMaterialService;

    @PreAuthorize("@ss.hasPermi('wechat:material:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatMaterialVO> page = wechatMaterialService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatMaterialService.delete(id);
        return AjaxResult.success();
    }
}
