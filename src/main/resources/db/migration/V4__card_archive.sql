-- Migration for Card Archiving
ALTER TABLE card ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE card ADD COLUMN archived_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_card_archived ON card(archived);
