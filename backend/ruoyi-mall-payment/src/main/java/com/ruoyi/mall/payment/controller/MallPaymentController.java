package com.ruoyi.mall.payment.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.mall.payment.dto.MockConfirmRequest;
import com.ruoyi.mall.payment.dto.PaymentCreateRequest;
import com.ruoyi.mall.payment.service.MallPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MallPaymentController extends MallPaymentControllerSupport
{
    private final MallPaymentService mallPaymentService;

    @PostMapping("/mall/payments")
    public AjaxResult create(@Valid @RequestBody PaymentCreateRequest request)
    {
        return AjaxResult.success(mallPaymentService.create(request));
    }

    @PostMapping("/mall/payments/mock/confirm")
    public AjaxResult confirmMock(@Valid @RequestBody MockConfirmRequest request)
    {
        mallPaymentService.confirmMock(request.getPayNo());
        return AjaxResult.success();
    }

    @Anonymous
    @PostMapping("/public/mall/payments/notify/{channel}")
    public AjaxResult notify(@PathVariable String channel, @RequestHeader Map<String, String> headers,
            @RequestBody String body)
    {
        mallPaymentService.notify(channel, headers, body);
        return AjaxResult.success();
    }
}
