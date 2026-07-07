-- Applied automatically on startup by DatabaseMigrationService (004_saved_query_owner).
-- Kept for reference / manual recovery.
ALTER TABLE saved_query ADD COLUMN owner_id TEXT REFERENCES users(id);CREATE INDEX IF NOT EXISTS idx_saved_query_owner ON saved_query(owner_id);
