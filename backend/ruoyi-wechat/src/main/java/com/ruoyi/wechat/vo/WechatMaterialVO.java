package com.ruoyi.wechat.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WechatMaterialVO
{
    private Long id;
    private Long accountId;
    private String title;
    private String author;
    private String digest;
    private String mediaId;
    private String url;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
