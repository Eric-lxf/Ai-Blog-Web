package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatAutoReplyVO
{
    private Long id;
    private Long accountId;
    private String replyType;
    private String keyword;
    private String content;
    private Integer enabled;
    private Integer matchType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
