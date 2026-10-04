-- Migration for Password Reset Tokens
CREATE TABLE password_reset_token (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    token       TEXT NOT NULL UNIQUE,
    user_id     UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
    expiry_date TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token(user_id);