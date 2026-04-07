ALTER TABLE review
    ADD COLUMN updated_at DATETIME NULL AFTER created_at;

UPDATE review
SET updated_at = created_at
WHERE updated_at IS NULL;
