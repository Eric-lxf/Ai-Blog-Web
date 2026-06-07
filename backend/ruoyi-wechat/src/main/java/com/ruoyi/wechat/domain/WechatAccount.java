package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_account")
public class WechatAccount
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String appId;
    private String appSecret;
    private String token;
    private String aesKey;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
