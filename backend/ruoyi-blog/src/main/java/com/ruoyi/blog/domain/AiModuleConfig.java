package com.ruoyi.blog.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("ai_module_config")
public class AiModuleConfig
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String moduleCode;

    private Long providerId;

    private String textModel;

    private String visionModel;

    private BigDecimal temperature;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
