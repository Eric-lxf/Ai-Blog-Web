package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatPublishRecordVO
{
    private Long id;
    private Long accountId;
    private Long articleId;
    private Long materialId;
    private String publishMode;
    private String msgId;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
