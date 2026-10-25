-- Flattened schema (Google-only sign-in; further identity providers add rows to user_provider).
-- Apply to an EMPTY database. An existing database that already ran the old V1-V6 migrations
-- must be dropped/recreated, because Flyway will reject the changed V1 checksum.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION uuidv7() RETURNS uuid AS $$
DECLARE
  timestamp_ms bigint;
  random_bytes bytea;
  hex_str      text;
BEGIN
  timestamp_ms := (extract(epoch from clock_timestamp()) * 1000)::bigint;
  random_bytes := gen_random_bytes(10);
  hex_str := encode(random_bytes, 'hex');

  RETURN (
    lpad(to_hex(timestamp_ms), 12, '0') ||
    '7' ||
    substring(hex_str from 1 for 3) ||
    (case (get_byte(random_bytes, 6) & 0x3)
      when 0 then '8'
      when 1 then '9'
      when 2 then 'a'
      else 'b'
     end) ||
    substring(hex_str from 4 for 15)
  )::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- ---------------------------------------------------------------------------------------------
-- Users and external identities
-- ---------------------------------------------------------------------------------------------
CREATE TABLE user_entity (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per (provider, subject). provider_id is the provider's stable subject id ("sub"),
-- never the email. The UNIQUE constraints also create the lookup indexes.
CREATE TABLE user_provider (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id         UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
    provider_type   VARCHAR(50)  NOT NULL,
    provider_id     VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_user_provider  UNIQUE (user_id, provider_type),
    CONSTRAINT unique_oauth_identity UNIQUE (provider_type, provider_id)
);

-- ---------------------------------------------------------------------------------------------
-- Boards, membership, columns, cards, comments
-- ---------------------------------------------------------------------------------------------
CREATE TABLE board (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    name        TEXT NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE board_member (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    board_id    UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (board_id, user_id)
);

CREATE INDEX idx_board_member_user ON board_member(user_id);

CREATE TABLE column_entity (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    board_id    UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    position    INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_column_board ON column_entity(board_id, position);

CREATE TABLE card (
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    column_id     UUID NOT NULL REFERENCES column_entity(id) ON DELETE CASCADE,
    title         TEXT NOT NULL,
    description   TEXT NOT NULL DEFAULT '',
    priority      TEXT NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH')),
    due_date      DATE,
    labels        JSONB NOT NULL DEFAULT '[]'::jsonb,
    position      INTEGER NOT NULL,
    archived      BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at   TIMESTAMPTZ,   -- set only while archived; NULL after restore
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_card_column ON card(column_id, position);
CREATE INDEX idx_card_archived ON card(archived);

CREATE TABLE card_comment (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    card_id     UUID NOT NULL REFERENCES card(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES user_entity(id),
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_card_comment_card_id ON card_comment(card_id);
CREATE INDEX idx_card_comment_created_at ON card_comment(created_at);
