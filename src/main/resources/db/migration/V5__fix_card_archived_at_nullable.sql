-- V4 incorrectly made archived_at NOT NULL with a default of now().
-- archived_at should only hold a value once a card is actually archived;
-- restoring a card sets it back to NULL, which violated that constraint.
ALTER TABLE card ALTER COLUMN archived_at DROP NOT NULL;
ALTER TABLE card ALTER COLUMN archived_at DROP DEFAULT;

-- Clean up existing rows: any non-archived card was incorrectly stamped
-- with an archived_at timestamp (its creation time) by the old default.
UPDATE card SET archived_at = NULL WHERE archived = FALSE;
