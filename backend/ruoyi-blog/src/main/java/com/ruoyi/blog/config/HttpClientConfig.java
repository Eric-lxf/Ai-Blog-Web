package com.ruoyi.blog.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

@Configuration
public class HttpClientConfig
{
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    @Bean
    public OkHttpClient deepSeekOkHttpClient()
    {
        return new OkHttpClient.Builder().readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
    }
}
