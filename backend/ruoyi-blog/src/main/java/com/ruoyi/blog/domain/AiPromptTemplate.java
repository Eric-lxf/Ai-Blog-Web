package com.ruoyi.blog.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate
{

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String sceneType;
    private String systemPrompt;
    private String modelName;
    private BigDecimal temperature;
    private Integer isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
