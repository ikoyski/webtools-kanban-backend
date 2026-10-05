package com.ikoyki.webtools.kanban.backend.dto.request;

/**
 * Payload for {@code POST /email/v1} on the Email microservice. Field names match its JSON contract.
 * Plain-text body; the caller composes subject and body.
 */
public record SendEmailRequest(String recipient, String msgBody, String subject) {
}
