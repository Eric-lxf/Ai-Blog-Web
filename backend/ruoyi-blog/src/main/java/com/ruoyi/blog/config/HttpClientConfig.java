package com.ruoyi.blog.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

@Configuration
public class HttpClientConfig
{

    @Bean
    public OkHttpClient deepSeekOkHttpClient(DeepSeekProperties deepSeekProperties)
    {
        return new OkHttpClient.Builder().readTimeout(deepSeekProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
    }
}
