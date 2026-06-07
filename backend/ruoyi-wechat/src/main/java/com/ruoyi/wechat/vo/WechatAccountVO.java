package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatAccountVO
{
    private Long id;
    private String name;
    private String appId;
    private String token;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
