package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatMenuVO
{
    private Long id;
    private Long accountId;
    private String menuJson;
    private Integer isPublished;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
