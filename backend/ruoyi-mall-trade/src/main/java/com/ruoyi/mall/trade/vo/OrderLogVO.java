package com.ruoyi.mall.trade.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderLogVO
{
    private Long id;
    private Long orderId;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
}
