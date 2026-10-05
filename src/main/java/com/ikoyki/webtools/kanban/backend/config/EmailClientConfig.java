package com.ikoyki.webtools.kanban.backend.config;

import com.ikoyki.webtools.kanban.backend.client.EmailClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(EmailClientProperties.class)
public class EmailClientConfig {

    static final String EMAIL_EXECUTOR = "emailExecutor";

    /**
     * Load-balanced builder: lets "http://webtools-email" resolve through Eureka. Kept separate from the plain
     * {@code RestTemplate} bean used for Cloudflare Turnstile, which must NOT be load-balanced.
     * Going through {@code RestClientBuilderConfigurer} keeps Boot's tracing/observation customizers applied,
     * so trace ids propagate to the Email service (Zipkin).
     */
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    EmailClient emailClient(@LoadBalanced RestClient.Builder builder, EmailClientProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(props.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());

        builder.baseUrl(props.baseUrl()).requestFactory(requestFactory);
        if (StringUtils.hasText(props.apiKey())) {
            builder.defaultHeader("X-Internal-Api-Key", props.apiKey());
        }

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(EmailClient.class);
    }

    /**
     * Small bounded pool so an Email outage can't pile up threads; when the queue is full, notifications are dropped
     * (and logged) instead of blocking user requests. Built from Boot's builder so tracing context is propagated.
     */
    @Bean(EMAIL_EXECUTOR)
    ThreadPoolTaskExecutor emailExecutor(ThreadPoolTaskExecutorBuilder builder) {
        ThreadPoolTaskExecutor executor = builder
                .corePoolSize(2)
                .maxPoolSize(4)
                .queueCapacity(100)
                .threadNamePrefix("email-")
                .build();
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("Email queue is full; dropping an outgoing email"));
        return executor;
    }
}
