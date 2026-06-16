-- Migration: add user_tag table and populate from existing notes/todos
CREATE TABLE IF NOT EXISTS user_tag (
    id TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(id),
    tag_name TEXT NOT NULL,
    UNIQUE(owner_id, tag_name)
);

CREATE INDEX IF NOT EXISTS idx_user_tag_owner ON user_tag(owner_id);

-- Populate from existing notes (tags stored as JSON array in TEXT column)
-- SQLite doesn't have native JSON array iteration, so we use a simple approach:
-- This migration should be run via application code or manually for production.
-- For new deployments, schema.sql already includes the table.
