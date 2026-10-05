package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.client.EmailClient;
import com.ikoyki.webtools.kanban.backend.dto.request.SendEmailRequest;
import com.ikoyki.webtools.kanban.backend.event.PasswordResetRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private EmailClient emailClient;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailClient, "https://app.example.com/reset-password");
    }

    @Test
    void passwordReset_sendsEmailWithResetLink() {
        emailService.onPasswordResetRequested(new PasswordResetRequestedEvent("user@example.com", "abc-123"));

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(emailClient).send(captor.capture());

        SendEmailRequest sent = captor.getValue();
        assertEquals("user@example.com", sent.recipient());
        assertEquals("Password Reset Request", sent.subject());
        assertTrue(sent.msgBody().contains("https://app.example.com/reset-password?token=abc-123"));
    }

    @Test
    void passwordReset_emailServiceDown_doesNotPropagate() {
        doThrow(new ResourceAccessException("connection refused")).when(emailClient).send(any());

        assertDoesNotThrow(() ->
                emailService.onPasswordResetRequested(new PasswordResetRequestedEvent("user@example.com", "abc-123")));
    }
}
