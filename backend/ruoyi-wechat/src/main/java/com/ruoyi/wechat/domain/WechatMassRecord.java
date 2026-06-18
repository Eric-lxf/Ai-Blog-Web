package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_mass_record")
public class WechatMassRecord
{
    @TableId(type = IdType.AUTO)
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
