package com.ikoyki.webtools.kanban.backend.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikoyki.webtools.kanban.backend.dto.response.InternalUserResponse;
import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.entity.UserProviderEntity;
import com.ikoyki.webtools.kanban.backend.repository.UserProviderRepository;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Identity operations for the auth server (internal API only).
 *
 * The auth server authenticates people with an external identity provider and then asks this
 * service which Kanban user that identity belongs to, creating the user on first sign-in.
 *
 * Linking rules:
 *  - an identity is (provider, subject); the email is never used to identify a person;
 *  - the provider must assert a verified email;
 *  - a NEW identity whose email already belongs to another user is refused (CONFLICT), not merged:
 *    merging by email lets someone who controls an unverified address at another provider take over
 *    an account. Linking a second provider needs an explicit step from a signed-in session.
 */
@Service
@RequiredArgsConstructor
public class InternalUserService {

    /** Providers this deployment accepts. Add "MICROSOFT", "GITHUB", ... when they are enabled. */
    private static final Set<String> ALLOWED_PROVIDERS = Set.of("GOOGLE", "GITHUB");

    private static final int MAX_SUBJECT = 255;
    private static final int MAX_EMAIL = 255;
    private static final int MAX_DISPLAY_NAME = 100;
    private static final int MAX_AVATAR_URL = 512;

    private final UserRepository userRepository;
    private final UserProviderRepository providerRepository;

    public record FederatedResult(Status status, InternalUserResponse user) {
        public enum Status { OK, INVALID, EMAIL_NOT_VERIFIED, EMAIL_CONFLICT }

        static FederatedResult ok(InternalUserResponse user) { return new FederatedResult(Status.OK, user); }
        static FederatedResult of(Status status) { return new FederatedResult(status, null); }
    }

    /** Finds the user for an external identity, or creates one on first sign-in. */
    @Transactional
    public FederatedResult federated(String provider, String subject, String email, boolean emailVerified,
                                     String displayName, String avatarUrl) {
        String type = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        String sub = subject == null ? "" : subject.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_PROVIDERS.contains(type)
                || sub.isEmpty() || sub.length() > MAX_SUBJECT
                || mail.isEmpty() || mail.length() > MAX_EMAIL) {
            return FederatedResult.of(FederatedResult.Status.INVALID);
        }
        if (!emailVerified) {
            return FederatedResult.of(FederatedResult.Status.EMAIL_NOT_VERIFIED);
        }

        // 1. Known identity: sign in as that user. Fill in profile fields only if still empty.
        Optional<UserProviderEntity> existing = providerRepository.findByProviderTypeAndProviderId(type, sub);
        if (existing.isPresent()) {
            UserEntity user = existing.get().getUser();
            if (user.getDisplayName() == null) {
                user.setDisplayName(cleanName(displayName));
            }
            if (user.getAvatarUrl() == null) {
                user.setAvatarUrl(cleanAvatar(avatarUrl));
            }
            return FederatedResult.ok(toResponse(user));
        }

        // 2. New identity whose email already belongs to someone: refuse instead of merging.
        if (userRepository.findByEmail(mail).isPresent()) {
            return FederatedResult.of(FederatedResult.Status.EMAIL_CONFLICT);
        }

        // 3. First sign-in: create the user and the identity row.
        UserEntity user = new UserEntity();
        user.setEmail(mail);
        user.setDisplayName(cleanName(displayName));
        user.setAvatarUrl(cleanAvatar(avatarUrl));
        user = userRepository.saveAndFlush(user);

        UserProviderEntity identity = new UserProviderEntity();
        identity.setUser(user);
        identity.setProviderType(type);
        identity.setProviderId(sub);
        providerRepository.saveAndFlush(identity);

        return FederatedResult.ok(toResponse(user));
    }

    /** Looks a user up by id (preferred) or by email. */
    @Transactional(readOnly = true)
    public Optional<InternalUserResponse> lookup(String id, String email) {
        if (id != null && !id.isBlank()) {
            try {
                return userRepository.findById(UUID.fromString(id.trim())).map(this::toResponse);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).map(this::toResponse);
        }
        return Optional.empty();
    }

    private InternalUserResponse toResponse(UserEntity user) {
        return new InternalUserResponse(user.getId().toString(), user.getEmail());
    }

    private static String cleanName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        return v.length() > MAX_DISPLAY_NAME ? v.substring(0, MAX_DISPLAY_NAME) : v;
    }

    private static String cleanAvatar(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_AVATAR_URL) {
            return null;
        }
        return value.trim();
    }
}
