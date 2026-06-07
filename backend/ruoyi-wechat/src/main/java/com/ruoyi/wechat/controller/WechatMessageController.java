package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatMessageService;
import com.ruoyi.wechat.vo.WechatMessageLogVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/message")
public class WechatMessageController extends WechatControllerSupport
{
    private final WechatMessageService wechatMessageService;

    @PreAuthorize("@ss.hasPermi('wechat:message:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatMessageLogVO> page = wechatMessageService.page(query);
        return mpPageTable(page);
    }
}
