package com.ikoyki.webtools.kanban.backend.event;

/**
 * Published inside the forgot-password transaction; the email is sent only after that transaction commits.
 * Carries plain values (not entities) because it is handled on another thread, outside the persistence context.
 */
public record PasswordResetRequestedEvent(String email, String token) {
}
