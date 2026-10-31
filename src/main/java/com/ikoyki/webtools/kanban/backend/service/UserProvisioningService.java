package com.ikoyki.webtools.kanban.backend.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Creates the app-local Kanban profile for a centrally authenticated identity.
 * The canonical UUID is supplied by the trusted gateway from the Auth Server's signed token.
 * This service stores no provider subject and makes no application authorization decisions.
 */
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final JdbcTemplate jdbc;
    private final UserRepository users;

    @Transactional
    public UserEntity ensureLocalUser(UUID centralUserId, String email, String displayName, String avatarUrl) {
        if (centralUserId == null || email == null || email.isBlank() || email.length() > 255) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid central identity claims");
        }

        // The Auth Server enforces global email uniqueness. The local row is keyed by the stable
        // central UUID; repeated API requests are idempotent and do not overwrite app-edited profile fields.
        jdbc.update("""
                INSERT INTO user_entity (id, email, display_name, avatar_url)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                   SET email = EXCLUDED.email,
                       display_name = COALESCE(user_entity.display_name, EXCLUDED.display_name),
                       avatar_url = COALESCE(user_entity.avatar_url, EXCLUDED.avatar_url),
                       updated_at = now()
                """, centralUserId, email.trim().toLowerCase(Locale.ROOT), clean(displayName, 100), clean(avatarUrl, 512));

        return users.findById(centralUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Could not provision the Kanban user profile"));
    }

    private static String clean(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            return null;
        }
        return value.trim();
    }
}
