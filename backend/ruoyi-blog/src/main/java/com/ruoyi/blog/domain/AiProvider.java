package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("ai_provider")
public class AiProvider
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** openai_compatible | anthropic */
    private String providerType;

    private String apiKey;

    /** api_key | auth_token，仅 Anthropic 使用 */
    private String authMode;

    private String baseUrl;

    private String defaultModel;

    private String visionModel;

    private Integer timeoutSeconds;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
