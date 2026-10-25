package com.ikoyki.webtools.kanban.backend.dto.response;

/** The signed-in user's profile, returned by GET /v1/users/me. */
public record UserProfileResponse(String id, String email, String displayName, String avatarUrl) {
}
