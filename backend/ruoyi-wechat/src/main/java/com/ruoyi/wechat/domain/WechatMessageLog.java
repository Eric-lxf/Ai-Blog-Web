package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_message_log")
public class WechatMessageLog
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String direction;
    private String openId;
    private String messageType;
    private String eventType;
    private String content;
    private String rawXml;
    private LocalDateTime createTime;
}
