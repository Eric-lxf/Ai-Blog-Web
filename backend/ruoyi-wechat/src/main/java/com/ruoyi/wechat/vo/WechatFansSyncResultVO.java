package com.ruoyi.wechat.vo;

import lombok.Data;

@Data
public class WechatFansSyncResultVO
{
    private Long accountId;
    private int synced;
    private int total;
    /** wechat_api | message_log */
    private String source;
    private String warning;
}
