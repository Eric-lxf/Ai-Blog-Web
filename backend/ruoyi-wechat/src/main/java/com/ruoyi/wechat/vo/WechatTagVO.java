package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class WechatTagVO
{
    private Long id;
    private Long accountId;
    private Integer wechatTagId;
    private String name;
    private Integer fanCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
