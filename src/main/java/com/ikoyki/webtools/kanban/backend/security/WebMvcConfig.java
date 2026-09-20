package com.ikoyki.webtools.kanban.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final List<HandlerMethodArgumentResolver> argumentResolvers;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // The AuthUserArgumentResolver will be automatically picked up
        // from the application context and added here.
        resolvers.addAll(argumentResolvers);
    }
}
