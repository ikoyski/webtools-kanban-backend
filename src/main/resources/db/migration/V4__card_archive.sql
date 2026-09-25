-- Migration for Card Archiving
ALTER TABLE card ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE card ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_card_archived ON card(archived);
