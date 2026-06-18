package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_qrcode")
public class WechatQrcode
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String name;
    private String qrType;
    private String sceneType;
    private Integer sceneId;
    private String sceneStr;
    private String actionName;
    private String ticket;
    private String url;
    private Integer expireSeconds;
    private LocalDateTime expireTime;
    private Integer scanCount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
