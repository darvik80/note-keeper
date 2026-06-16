-- Migration: fix null owner_id records and add NOT NULL constraint
-- Step 1: Delete orphaned notes/todos with no owner (data created before auth was enforced)
DELETE FROM note_history WHERE note_id IN (SELECT id FROM note WHERE owner_id IS NULL);
DELETE FROM attachment WHERE parent_id IN (SELECT id FROM note WHERE owner_id IS NULL) AND parent_type = 'note';
DELETE FROM note WHERE owner_id IS NULL;

DELETE FROM attachment WHERE parent_id IN (SELECT id FROM todo WHERE owner_id IS NULL) AND parent_type = 'todo';
DELETE FROM todo WHERE owner_id IS NULL;

-- Note: SQLite does not support ALTER COLUMN to add NOT NULL on existing columns.
-- The NOT NULL constraint is enforced in the updated schema.sql for new deployments.
-- For existing databases, the DELETE above ensures no nulls remain.
