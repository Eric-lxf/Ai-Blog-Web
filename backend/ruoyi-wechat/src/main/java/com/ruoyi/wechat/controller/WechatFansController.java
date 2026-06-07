package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatFansService;
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
}
