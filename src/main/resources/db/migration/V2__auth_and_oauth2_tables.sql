-- V2__users_and_boards_membership.sql

-- 1. Main User Profile Table
CREATE TABLE user_entity (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Authentication Provider Table
CREATE TABLE user_provider (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id         UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
    provider_type   VARCHAR(50) NOT NULL,
    provider_id     VARCHAR(255),
    password_hash   VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT      unique_user_provider UNIQUE (user_id, provider_type),
    CONSTRAINT      unique_oauth_identity UNIQUE (provider_type, provider_id)
);

CREATE INDEX idx_user_provider_search ON user_provider(provider_type, provider_id);

-- 3. Board Membership
CREATE TABLE board_member (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    board_id    UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (board_id, user_id)
);

CREATE INDEX idx_board_member_user ON board_member(user_id);
CREATE INDEX idx_board_member_board ON board_member(board_id);
