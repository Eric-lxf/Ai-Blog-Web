package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatQrcodeVO
{
    private Long id;
    private Long accountId;
    private String name;
    private String qrType;
    private String sceneType;
    private Integer sceneId;
    private String sceneStr;
    private String actionName;
    private String ticket;
    private String url;
    private String imageUrl;
    private Integer expireSeconds;
    private LocalDateTime expireTime;
    private Integer scanCount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
