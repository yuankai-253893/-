package com.yuankai.aispringboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 读取配置文件的路径
    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL路径 /files/bussiness/** 映射到本地 upload/files/bussiness/ 目录
        registry.addResourceHandler("/files/bussiness/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
