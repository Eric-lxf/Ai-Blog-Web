package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatMediaAssetVO
{
    private Long id;
    private Long accountId;
    private String name;
    private String mediaType;
    private String mediaId;
    private String url;
    private String fileName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
