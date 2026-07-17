package com.ruoyi.mall.trade.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminOrderPageQuery
{
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String status;
    private String orderNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
