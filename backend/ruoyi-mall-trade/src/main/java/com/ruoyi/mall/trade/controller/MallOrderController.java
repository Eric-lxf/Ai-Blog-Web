package com.ruoyi.mall.trade.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.mall.trade.dto.OrderCreateRequest;
import com.ruoyi.mall.trade.dto.OrderPageQuery;
import com.ruoyi.mall.trade.service.MallOrderService;
import com.ruoyi.mall.trade.vo.OrderVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/orders")
@RequiredArgsConstructor
public class MallOrderController extends MallTradeControllerSupport
{
    private final MallOrderService mallOrderService;

    @PostMapping
    public AjaxResult create(@Valid @RequestBody OrderCreateRequest request)
    {
        return AjaxResult.success(mallOrderService.create(request));
    }

    @GetMapping
    public TableDataInfo page(@Valid OrderPageQuery query)
    {
        Page<OrderVO> page = mallOrderService.pageMine(query);
        return mpPageTable(page);
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallOrderService.getMine(id));
    }

    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id)
    {
        mallOrderService.cancelMine(id);
        return AjaxResult.success();
    }
}
