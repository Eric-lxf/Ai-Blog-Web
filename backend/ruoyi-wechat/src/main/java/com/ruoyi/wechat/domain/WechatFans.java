package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_fans")
public class WechatFans
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String openId;
    private String unionId;
    private String nickname;
    private Integer subscribeStatus;
    private LocalDateTime subscribeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
