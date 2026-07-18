package com.ruoyi.mall.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.mall.trade.dto.AdminOrderPageQuery;
import com.ruoyi.mall.trade.dto.OrderCreateRequest;
import com.ruoyi.mall.trade.dto.OrderPageQuery;
import com.ruoyi.mall.trade.vo.MallOrderPaymentView;
import com.ruoyi.mall.trade.vo.OrderVO;

public interface MallOrderService
{
    OrderVO create(OrderCreateRequest request);

    Page<OrderVO> pageMine(OrderPageQuery query);

    OrderVO getMine(Long id);

    void cancelMine(Long id);

    Page<OrderVO> adminPage(AdminOrderPageQuery query);

    OrderVO adminDetail(Long id);

    void ship(Long id);

    void complete(Long id);

    void cancelExpiredOrders();

    void markOrderPaid(Long orderId, String payNo);

    MallOrderPaymentView getPayableOrder(Long orderId, Long userId);
}
