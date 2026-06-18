package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_tag")
public class WechatTag
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Integer wechatTagId;
    private String name;
    private Integer fanCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
