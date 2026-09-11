CREATE TABLE board (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT NOT NULL DEFAULT 'My Board',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE column_entity (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    board_id    BIGINT NOT NULL REFERENCES board(id) ON DELETE CASCADE,
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
