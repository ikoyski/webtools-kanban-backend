-- V2__auth_and_oauth2_tables.sql

-- 1. Main User Profile Table
CREATE TABLE user_entity (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Authentication Provider Table (Allows multiple login types per user)
CREATE TABLE user_provider (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id         UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
    provider_type   VARCHAR(50) NOT NULL,   -- 'LOCAL', 'GOOGLE', 'GITHUB', etc.
    provider_id     VARCHAR(255),           -- The 'sub' ID from Google, or NULL for local
    password_hash   VARCHAR(255),           -- Extracted password hash for 'LOCAL', NULL for OAuth
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT      unique_user_provider UNIQUE (user_id, provider_type),
    CONSTRAINT      unique_oauth_identity UNIQUE (provider_type, provider_id)
);

CREATE INDEX idx_user_provider_search ON user_provider(provider_type, provider_id);
