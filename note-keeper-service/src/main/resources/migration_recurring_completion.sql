-- Applied automatically on startup by DatabaseMigrationService (008_recurring_completion).
-- Kept for reference / manual recovery.

-- Completion history for recurring todos (calendar view)
CREATE TABLE IF NOT EXISTS todo_completion_log (
    id TEXT PRIMARY KEY,
    todo_id TEXT NOT NULL REFERENCES todo(id) ON DELETE CASCADE,
    completed_at TEXT NOT NULL,
    occurrence_reminder TEXT,
    occurrence_due_date TEXT
);

CREATE INDEX IF NOT EXISTS idx_completion_log_todo ON todo_completion_log(todo_id);
CREATE INDEX IF NOT EXISTS idx_completion_log_date ON todo_completion_log(completed_at);

-- Denormalized: when this recurring todo was last completed
ALTER TABLE todo ADD COLUMN last_completed_at TEXT;

-- Fix existing stuck recurring todos: reset completed so they become visible again
UPDATE todo SET completed = 0, notified_at = NULL
WHERE schedule_repeat IN ('daily', 'weekly', 'monthly')
  AND completed = 1
  AND is_deleted = 0;
