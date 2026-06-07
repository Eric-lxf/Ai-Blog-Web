package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_material")
public class WechatMaterial
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String title;
    private String thumbMediaId;
    private String author;
    private String digest;
    private String content;
    private String contentSourceUrl;
    private String mediaId;
    private String url;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
