CREATE TABLE IF NOT EXISTS note (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    tags TEXT DEFAULT '[]',
    folder TEXT NOT NULL DEFAULT 'default',
    subfolder TEXT,
    priority TEXT NOT NULL DEFAULT 'medium' CHECK(priority IN ('low','medium','high')),
    is_favorite INTEGER NOT NULL DEFAULT 0,
    is_encrypted INTEGER NOT NULL DEFAULT 0,
    is_archived INTEGER NOT NULL DEFAULT 0,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at TEXT,
    reminder TEXT,
    template_id TEXT,
    owner_id TEXT REFERENCES users(id),
    shared_with TEXT DEFAULT '[]',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_note_folder ON note(folder);
CREATE INDEX IF NOT EXISTS idx_note_is_deleted ON note(is_deleted);
CREATE INDEX IF NOT EXISTS idx_note_is_archived ON note(is_archived);
CREATE INDEX IF NOT EXISTS idx_note_priority ON note(priority);
CREATE INDEX IF NOT EXISTS idx_note_created_at ON note(created_at);

CREATE TABLE IF NOT EXISTS note_history (
    id TEXT PRIMARY KEY,
    note_id TEXT NOT NULL,
    content TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    action TEXT NOT NULL CHECK(action IN ('created','edited','restored')),
    FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_note_history_note_id ON note_history(note_id);

CREATE TABLE IF NOT EXISTS todo (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    completed INTEGER NOT NULL DEFAULT 0,
    tags TEXT DEFAULT '[]',
    priority TEXT NOT NULL DEFAULT 'medium' CHECK(priority IN ('low','medium','high')),
    is_favorite INTEGER NOT NULL DEFAULT 0,
    is_archived INTEGER NOT NULL DEFAULT 0,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    deleted_at TEXT,
    due_date TEXT,
    reminder TEXT,
    notified_at TEXT,
    notification_channels TEXT,
    location_lat REAL,
    location_lng REAL,
    location_address TEXT,
    schedule_repeat TEXT DEFAULT 'none' CHECK(schedule_repeat IN ('none','daily','weekly','monthly')),
    schedule_end_date TEXT,
    owner_id TEXT REFERENCES users(id),
    shared_with TEXT DEFAULT '[]',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_todo_completed ON todo(completed);
CREATE INDEX IF NOT EXISTS idx_todo_is_deleted ON todo(is_deleted);
CREATE INDEX IF NOT EXISTS idx_todo_is_archived ON todo(is_archived);
CREATE INDEX IF NOT EXISTS idx_todo_due_date ON todo(due_date);
CREATE INDEX IF NOT EXISTS idx_todo_created_at ON todo(created_at);

CREATE TABLE IF NOT EXISTS attachment (
    id TEXT PRIMARY KEY,
    parent_id TEXT NOT NULL,
    parent_type TEXT NOT NULL CHECK(parent_type IN ('note','todo')),
    name TEXT NOT NULL,
    size INTEGER NOT NULL,
    type TEXT NOT NULL,
    url TEXT NOT NULL,
    uploaded_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attachment_parent ON attachment(parent_id, parent_type);

CREATE TABLE IF NOT EXISTS note_template (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    tags TEXT DEFAULT '[]',
    category TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_query (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    query TEXT NOT NULL,
    filter_type TEXT,
    filter_tags TEXT DEFAULT '[]',
    filter_priority TEXT,
    filter_folder TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_settings (
    id TEXT PRIMARY KEY DEFAULT 'default',
    telegram_bot_token TEXT,
    telegram_chat_id TEXT,
    dingtalk_webhook TEXT,
    dingtalk_secret TEXT,
    backup_auto_enabled INTEGER NOT NULL DEFAULT 0,
    backup_cron TEXT DEFAULT '0 0 2 * * *',
    backup_retention_days INTEGER NOT NULL DEFAULT 30,
    updated_at TEXT NOT NULL
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    avatar_url TEXT,
    provider TEXT NOT NULL DEFAULT 'local' CHECK(provider IN ('local', 'google')),
    google_id TEXT UNIQUE,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- User credentials for password authentication
CREATE TABLE IF NOT EXISTS user_credentials (
    user_id TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_note_owner ON note(owner_id);
CREATE INDEX IF NOT EXISTS idx_todo_owner ON todo(owner_id);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);
