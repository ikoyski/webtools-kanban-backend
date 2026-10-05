package com.ikoyki.webtools.kanban.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings for calling the Email microservice directly (service-to-service, not through the API gateway).
 *
 * @param baseUrl        Eureka application name of the Email service (resolved by Spring Cloud LoadBalancer)
 * @param apiKey         optional shared secret sent as {@code X-Internal-Api-Key}; omitted when blank
 * @param connectTimeout max time to establish a connection
 * @param readTimeout    max time to wait for the Email service to respond
 */
@ConfigurationProperties(prefix = "webtools.email")
public record EmailClientProperties(
        @DefaultValue("http://webtools-email") String baseUrl,
        String apiKey,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {
}
