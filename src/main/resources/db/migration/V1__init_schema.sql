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

CREATE TABLE board (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    name        TEXT NOT NULL DEFAULT 'My Board',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE column_entity (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    board_id    UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    position    INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE card (
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    column_id     UUID NOT NULL REFERENCES column_entity(id) ON DELETE CASCADE,
    title         TEXT NOT NULL,
    description   TEXT NOT NULL DEFAULT '',
    priority      TEXT NOT NULL DEFAULT 'Medium' CHECK (priority IN ('Low','Medium','High')),
    due_date      DATE,
    labels        JSONB NOT NULL DEFAULT '[]'::jsonb,
    position      INTEGER NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_column_board ON column_entity(board_id, position);
CREATE INDEX idx_card_column ON card(column_id, position);
