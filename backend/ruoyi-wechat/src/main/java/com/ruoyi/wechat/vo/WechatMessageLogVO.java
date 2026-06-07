package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatMessageLogVO
{
    private Long id;
    private Long accountId;
    private String direction;
    private String openId;
    private String messageType;
    private String eventType;
    private String content;
    private LocalDateTime createTime;
}
