package com.ruoyi.blog.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

@Configuration
public class HttpClientConfig
{
    /** 默认读超时（秒），与 Provider 未指定 timeout 时一致 */
    public static final int DEFAULT_READ_TIMEOUT_SECONDS = 300;

    @Bean
    public OkHttpClient deepSeekOkHttpClient()
    {
        return new OkHttpClient.Builder()
                .readTimeout(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
