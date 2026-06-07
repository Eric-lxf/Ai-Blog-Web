package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatFansVO
{
    private Long id;
    private Long accountId;
    private String openId;
    private String unionId;
    private String nickname;
    private Integer subscribeStatus;
    private LocalDateTime subscribeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
