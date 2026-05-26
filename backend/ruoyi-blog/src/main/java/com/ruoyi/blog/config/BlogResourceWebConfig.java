package com.ruoyi.blog.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BlogResourceWebConfig implements WebMvcConfigurer
{

    private final BlogFileProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        String location = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().toUri().toString();
        String prefix = fileStorageProperties.getUrlPrefix();
        if (!prefix.startsWith("/"))
        {
            prefix = "/" + prefix;
        }
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
