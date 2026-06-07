package com.ruoyi.wechat.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("wx_publish_record")
public class WechatPublishRecord
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long articleId;
    private Long materialId;
    private String publishMode;
    private String msgId;
    private String responseBody;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
