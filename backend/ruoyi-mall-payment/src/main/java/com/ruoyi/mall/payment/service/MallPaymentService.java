package com.ruoyi.mall.payment.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.mall.payment.domain.MallPaymentOrder;
import com.ruoyi.mall.payment.dto.PaymentCreateRequest;
import com.ruoyi.mall.payment.dto.PaymentPageQuery;
import com.ruoyi.mall.payment.gateway.PaymentCreateResult;

public interface MallPaymentService
{
    PaymentCreateResult create(PaymentCreateRequest request);

    void confirmMock(String payNo);

    void notify(String channel, Map<String, String> headers, String body);

    Page<MallPaymentOrder> adminPage(PaymentPageQuery query);

    MallPaymentOrder adminDetail(Long id);
}
