package com.ruoyi.blog.config;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AiExecutorConfig
{

    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor()
    {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        return new ThreadPoolExecutor(cores, cores * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(200), r -> {
            Thread t = new Thread(r);
            t.setName("ai-task-" + t.getId());
            t.setDaemon(true);
            return t;
        }, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
