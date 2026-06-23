package com.ruoyi.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties
{

    private String apiKey;

    /** API 地址 */
    private String baseUrl = "https://api.deepseek.com";

    private String model = "deepseek-chat";

    private int timeoutSeconds = 300;

    /** 用于图片识别的视觉模型，需支持 OpenAI vision 格式（如 deepseek-vl2、gpt-4o 等） */
    private String visionModel = "deepseek-vl2";

    public boolean isConfigured()
    {
        return apiKey != null && !apiKey.isBlank();
    }
}
