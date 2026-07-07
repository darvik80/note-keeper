-- Applied automatically on startup by DatabaseMigrationService (005_template_owner).
-- Kept for reference / manual recovery.
ALTER TABLE note_template ADD COLUMN owner_id TEXT REFERENCES users(id);CREATE INDEX IF NOT EXISTS idx_note_template_owner ON note_template(owner_id);
