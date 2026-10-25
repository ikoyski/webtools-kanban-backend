package com.ikoyki.webtools.kanban.backend.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ikoyki.webtools.kanban.backend.dto.response.InternalUserResponse;
import com.ikoyki.webtools.kanban.backend.service.InternalUserService;

/**
 * Internal API for the auth server. It is NOT under /v1, so the gateway's /kanban-backend/v1/**
 * route never exposes it, and every call needs the shared X-Internal-Key. Keep this service's
 * port off the internet.
 *
 * Responses are built directly (no exceptions) so the global exception handler cannot change them.
 */
@RestController
@RequestMapping("/internal/v1/users")
public class InternalUserController {

    private final InternalUserService users;
    private final byte[] internalKey;

    public InternalUserController(InternalUserService users, @Value("${internal.api-key}") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("internal.api-key must be set");
        }
        this.users = users;
        this.internalKey = key.getBytes(StandardCharsets.UTF_8);
    }

    public record FederatedRequest(String provider, String subject, String email, Boolean emailVerified,
                                   String displayName, String avatarUrl) {}

    public record LookupRequest(String id, String email) {}

    @PostMapping("/federated")
    public ResponseEntity<?> federated(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestBody FederatedRequest request) {
        if (!keyValid(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        InternalUserService.FederatedResult result;
        try {
            result = run(request);
        } catch (DataIntegrityViolationException e) {
            // Two first sign-ins for the same identity raced; the loser retries and finds the winner's rows.
            result = run(request);
        }
        return switch (result.status()) {
            case OK -> ResponseEntity.ok(result.user());
            case INVALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid_request"));
            case EMAIL_NOT_VERIFIED -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "email_not_verified"));
            case EMAIL_CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "email_in_use"));
        };
    }

    @PostMapping("/lookup")
    public ResponseEntity<InternalUserResponse> lookup(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestBody LookupRequest request) {
        if (!keyValid(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return users.lookup(request.id(), request.email())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private InternalUserService.FederatedResult run(FederatedRequest r) {
        return users.federated(r.provider(), r.subject(), r.email(),
                Boolean.TRUE.equals(r.emailVerified()), r.displayName(), r.avatarUrl());
    }

    private boolean keyValid(String provided) {
        return provided != null
                && MessageDigest.isEqual(internalKey, provided.getBytes(StandardCharsets.UTF_8));
    }
}
