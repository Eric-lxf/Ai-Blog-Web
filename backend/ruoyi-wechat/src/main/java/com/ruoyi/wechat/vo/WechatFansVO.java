package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import java.util.List;

import lombok.Data;

@Data
public class WechatFansVO
{
    private Long id;
    private Long accountId;
    private String openId;
    private String unionId;
    private String nickname;
    private String remark;
    private Integer subscribeStatus;
    private List<String> tagNames;
    private LocalDateTime subscribeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
