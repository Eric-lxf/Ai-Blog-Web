package com.ruoyi.mall.payment.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.mall.payment.domain.MallPaymentOrder;
import com.ruoyi.mall.payment.dto.PaymentPageQuery;
import com.ruoyi.mall.payment.service.MallPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/admin/payments")
@RequiredArgsConstructor
public class MallAdminPaymentController extends MallPaymentControllerSupport
{
    private final MallPaymentService mallPaymentService;

    @PreAuthorize("@ss.hasPermi('mall:payment:list')")
    @GetMapping
    public TableDataInfo page(@Valid PaymentPageQuery query)
    {
        Page<MallPaymentOrder> page = mallPaymentService.adminPage(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('mall:payment:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallPaymentService.adminDetail(id));
    }
}
