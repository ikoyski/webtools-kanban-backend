package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.client.EmailClient;
import com.ikoyki.webtools.kanban.backend.dto.request.SendEmailRequest;
import com.ikoyki.webtools.kanban.backend.event.PasswordResetRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sends transactional emails by calling the Email microservice directly (Eureka name, no API gateway hop).
 * Delivery is best-effort: it runs off the request thread, after the DB transaction commits, and a failure is
 * logged but never surfaces to the caller. Do not log tokens or message bodies.
 */
@Slf4j
@Service
public class EmailService {

    private final EmailClient emailClient;
    private final String resetPasswordUrl;

    public EmailService(EmailClient emailClient,
                        @Value("${webtools.frontend.reset-password-url}")
                        String resetPasswordUrl) {
        this.emailClient = emailClient;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String resetLink = UriComponentsBuilder.fromUriString(resetPasswordUrl)
                .queryParam("token", event.token())
                .build()
                .toUriString();

        String body = "You requested a password reset for your account. Please click the link below to reset your password:\n\n"
                + resetLink + "\n\nThis link will expire in 1 hour.";

        send(new SendEmailRequest(event.email(), body, "Password Reset Request"));
    }

    private void send(SendEmailRequest request) {
        try {
            emailClient.send(request);
            log.info("Email '{}' sent to {}", request.subject(), request.recipient());
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", request.subject(), request.recipient(), e.getMessage());
        }
    }
}
