package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_menu")
public class WechatMenu
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String menuJson;
    private Integer isPublished;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
