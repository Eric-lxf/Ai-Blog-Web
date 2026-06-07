package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_auto_reply")
public class WechatAutoReply
{
    @TableId(type = IdType.AUTO)
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
