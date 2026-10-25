package com.ikoyki.webtools.kanban.backend.dto.response;

/** Minimal user identity returned to the auth server over the internal API. */
public record InternalUserResponse(String id, String email) {
}
