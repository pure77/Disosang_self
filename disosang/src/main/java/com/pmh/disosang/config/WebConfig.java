package com.pmh.disosang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir}")
    private String uploadDir; // application.properties에서 경로 주입

    @Value("${file.upload.url-prefix}")
    private String urlPrefix; // application.properties에서 URL 프리픽스 주입

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // [1] 웹 요청 URL: /images/reviews/** 패턴으로 요청이 오면
        // [2] 실제 경로: file:///home/upload/review-photos/ 에서 파일을 찾도록 매핑합니다.
        registry.addResourceHandler(urlPrefix + "**")
                .addResourceLocations("file:" + uploadDir);
    }
}
