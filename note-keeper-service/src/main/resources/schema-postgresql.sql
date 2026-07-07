-- PostgreSQL schema
-- Types: TEXT → VARCHAR/TEXT, INTEGER → BOOLEAN/INT, REAL → DOUBLE PRECISION

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    provider VARCHAR(50) NOT NULL DEFAULT 'local' CHECK(provider IN ('local', 'google')),
    google_id VARCHAR(255) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

CREATE TABLE IF NOT EXISTS user_credentials (
    user_id VARCHAR(255) PRIMARY KEY,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS note (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    tags TEXT DEFAULT '[]',
    folder VARCHAR(255) NOT NULL DEFAULT 'default',
    subfolder VARCHAR(255),
    priority VARCHAR(50) NOT NULL DEFAULT 'medium' CHECK(priority IN ('low','medium','high')),
    is_favorite BOOLEAN NOT NULL DEFAULT false,
    is_encrypted BOOLEAN NOT NULL DEFAULT false,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    reminder TIMESTAMP,
    template_id VARCHAR(255),
    owner_id VARCHAR(255) REFERENCES users(id),
    shared_with TEXT DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_note_folder ON note(folder);
CREATE INDEX IF NOT EXISTS idx_note_is_deleted ON note(is_deleted);
CREATE INDEX IF NOT EXISTS idx_note_is_archived ON note(is_archived);
CREATE INDEX IF NOT EXISTS idx_note_priority ON note(priority);
CREATE INDEX IF NOT EXISTS idx_note_created_at ON note(created_at);
CREATE INDEX IF NOT EXISTS idx_note_owner ON note(owner_id);

CREATE TABLE IF NOT EXISTS note_history (
    id VARCHAR(255) PRIMARY KEY,
    note_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action VARCHAR(50) NOT NULL CHECK(action IN ('created','edited','restored')),
    FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_note_history_note_id ON note_history(note_id);

CREATE TABLE IF NOT EXISTS todo (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    completed BOOLEAN NOT NULL DEFAULT false,
    tags TEXT DEFAULT '[]',
    priority VARCHAR(50) NOT NULL DEFAULT 'medium' CHECK(priority IN ('low','medium','high')),
    is_favorite BOOLEAN NOT NULL DEFAULT false,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    due_date TIMESTAMP,
    reminder TIMESTAMP,
    notified_at TIMESTAMP,
    notification_channels TEXT,
    location_lat DOUBLE PRECISION,
    location_lng DOUBLE PRECISION,
    location_address TEXT,
    schedule_repeat VARCHAR(50) DEFAULT 'none' CHECK(schedule_repeat IN ('none','daily','weekly','monthly')),
    schedule_end_date TIMESTAMP,
    owner_id VARCHAR(255) REFERENCES users(id),
    shared_with TEXT DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_todo_completed ON todo(completed);
CREATE INDEX IF NOT EXISTS idx_todo_is_deleted ON todo(is_deleted);
CREATE INDEX IF NOT EXISTS idx_todo_is_archived ON todo(is_archived);
CREATE INDEX IF NOT EXISTS idx_todo_due_date ON todo(due_date);
CREATE INDEX IF NOT EXISTS idx_todo_created_at ON todo(created_at);
CREATE INDEX IF NOT EXISTS idx_todo_owner ON todo(owner_id);

CREATE TABLE IF NOT EXISTS attachment (
    id VARCHAR(255) PRIMARY KEY,
    parent_id VARCHAR(255) NOT NULL,
    parent_type VARCHAR(50) NOT NULL CHECK(parent_type IN ('note','todo')),
    name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachment_parent ON attachment(parent_id, parent_type);

CREATE TABLE IF NOT EXISTS note_template (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    tags TEXT DEFAULT '[]',
    category VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS saved_query (
    id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    query TEXT NOT NULL,
    filter_type VARCHAR(255),
    filter_tags TEXT DEFAULT '[]',
    filter_priority VARCHAR(50),
    filter_folder VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_settings (
    id VARCHAR(255) PRIMARY KEY DEFAULT 'default',
    telegram_bot_token TEXT,
    telegram_chat_id TEXT,
    dingtalk_webhook TEXT,
    dingtalk_secret TEXT,
    backup_auto_enabled BOOLEAN NOT NULL DEFAULT false,
    backup_cron VARCHAR(100) DEFAULT '0 0 2 * * *',
    backup_retention_days INTEGER NOT NULL DEFAULT 30,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
