package com.ruoyi.wechat.vo;

import lombok.Data;

@Data
public class WechatFansSyncResultVO
{
    private Long accountId;
    private int synced;
    private int total;
}
