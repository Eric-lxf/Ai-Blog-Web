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
import com.ruoyi.wechat.dto.WechatAutoReplySaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatReplyService;
import com.ruoyi.wechat.vo.WechatAutoReplyVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/reply")
public class WechatReplyController extends WechatControllerSupport
{
    private final WechatReplyService wechatReplyService;

    @PreAuthorize("@ss.hasPermi('wechat:reply:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatAutoReplyVO> page = wechatReplyService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:reply:add') or @ss.hasPermi('wechat:reply:edit')")
    @PostMapping
    public AjaxResult save(@Valid @RequestBody WechatAutoReplySaveRequest request)
    {
        return AjaxResult.success(wechatReplyService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:reply:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatReplyService.delete(id);
        return AjaxResult.success();
    }
}
