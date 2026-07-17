package com.ruoyi.mall.payment.gateway;

import java.util.Map;

public interface PaymentGateway
{
    String channel();

    PaymentCreateResult create(PaymentCreateCmd cmd);

    NotifyResult parseNotify(Map<String, String> headers, String body);
}
