package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatMassRecordVO
{
    private Long id;
    private Long accountId;
    private String msgType;
    private String content;
    private String mediaId;
    private Integer isToAll;
    private Integer wechatTagId;
    private String status;
    private Long msgId;
    private String responseBody;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
