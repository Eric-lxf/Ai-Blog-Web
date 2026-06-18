package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_media_asset")
public class WechatMediaAsset
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String name;
    /** image / thumb / content */
    private String mediaType;
    private String mediaId;
    private String url;
    private String fileName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
