package com.ruoyi.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "blog.oss")
public class OssProperties
{

    /** 是否启用 OSS（false 时使用本地存储） */
    private boolean enabled = false;

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    /** 自定义域名（为空则使用 OSS 默认域名） */
    private String customDomain;
    /** OSS 内对象前缀目录，例如 uploads/ */
    private String objectPrefix = "uploads/";
}