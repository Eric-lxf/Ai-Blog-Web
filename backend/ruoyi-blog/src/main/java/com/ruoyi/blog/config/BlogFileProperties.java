package com.ruoyi.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "blog.file")
public class BlogFileProperties
{

    /** 本地上传目录 */
    private String uploadDir = "./uploads";

    /** 对外访问 URL 前缀 */
    private String urlPrefix = "/uploads";
}
