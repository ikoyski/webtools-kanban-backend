package com.ikoyki.webtools.kanban.backend.client;

import com.ikoyki.webtools.kanban.backend.dto.request.SendEmailRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client for the {@code webtools-email} microservice.
 * The path and payload shape live here, so adjusting to the Email service's real API is a change to this file
 * and {@code SendEmailRequest} only.
 */
@HttpExchange("/email/v1")
public interface EmailClient {

    @PostExchange
    void send(@RequestBody SendEmailRequest request);
}
