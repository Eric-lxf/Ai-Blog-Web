package com.ruoyi.mall.trade.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderVO
{
    private Long id;
    private String orderNo;
    private Long userId;
    private String status;
    private BigDecimal payAmount;
    private BigDecimal goodsAmount;
    private BigDecimal freightAmount;
    private String addressSnapshot;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private LocalDateTime expireTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrderItemVO> items;
    private List<OrderLogVO> logs;
}
