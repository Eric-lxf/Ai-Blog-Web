package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_fans_tag")
public class WechatFansTag
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fansId;
    private Long tagId;
}
