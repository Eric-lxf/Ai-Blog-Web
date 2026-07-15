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

    /**
     * 鉴权方式：api_key（x-api-key）| auth_token（Authorization Bearer，同 ANTHROPIC_AUTH_TOKEN）。
     * 仅 anthropic 生效；openai_compatible 忽略。
     */
    private String authMode;

    private String apiKey;

    private String baseUrl;

    private String defaultModel;

    private String visionModel;

    private Integer timeoutSeconds;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
