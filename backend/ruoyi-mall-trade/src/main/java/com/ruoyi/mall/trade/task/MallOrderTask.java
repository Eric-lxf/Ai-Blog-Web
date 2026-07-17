package com.ruoyi.mall.trade.task;

import org.springframework.stereotype.Component;

import com.ruoyi.mall.trade.service.MallOrderService;

import lombok.RequiredArgsConstructor;

@Component("mallOrderTask")
@RequiredArgsConstructor
public class MallOrderTask
{
    private final MallOrderService mallOrderService;

    public void cancelExpiredOrders()
    {
        mallOrderService.cancelExpiredOrders();
    }
}
