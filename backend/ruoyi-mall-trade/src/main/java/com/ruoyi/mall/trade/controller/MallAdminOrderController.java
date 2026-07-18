package com.ruoyi.mall.trade.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.mall.trade.dto.AdminOrderPageQuery;
import com.ruoyi.mall.trade.service.MallOrderService;
import com.ruoyi.mall.trade.vo.OrderVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/admin/orders")
@RequiredArgsConstructor
public class MallAdminOrderController extends MallTradeControllerSupport
{
    private final MallOrderService mallOrderService;

    @PreAuthorize("@ss.hasPermi('mall:order:list')")
    @GetMapping
    public TableDataInfo page(@Valid AdminOrderPageQuery query)
    {
        Page<OrderVO> page = mallOrderService.adminPage(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('mall:order:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallOrderService.adminDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:order:ship')")
    @PostMapping("/{id}/ship")
    public AjaxResult ship(@PathVariable Long id)
    {
        mallOrderService.ship(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:order:complete')")
    @PostMapping("/{id}/complete")
    public AjaxResult complete(@PathVariable Long id)
    {
        mallOrderService.complete(id);
        return AjaxResult.success();
    }
}
