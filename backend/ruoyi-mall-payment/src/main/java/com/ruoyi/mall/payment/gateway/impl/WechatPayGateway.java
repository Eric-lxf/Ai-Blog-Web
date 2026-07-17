package com.ruoyi.mall.payment.gateway.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.mall.payment.constant.MallPaymentConstants;
import com.ruoyi.mall.payment.gateway.NotifyResult;
import com.ruoyi.mall.payment.gateway.PaymentCreateCmd;
import com.ruoyi.mall.payment.gateway.PaymentCreateResult;
import com.ruoyi.mall.payment.gateway.PaymentGateway;

@Component
public class WechatPayGateway implements PaymentGateway
{
    @Override
    public String channel()
    {
        return MallPaymentConstants.CHANNEL_WECHAT;
    }

    @Override
    public PaymentCreateResult create(PaymentCreateCmd cmd)
    {
        throw new ServiceException("微信支付未配置");
    }

    @Override
    public NotifyResult parseNotify(Map<String, String> headers, String body)
    {
        throw new ServiceException("微信支付未配置");
    }
}
