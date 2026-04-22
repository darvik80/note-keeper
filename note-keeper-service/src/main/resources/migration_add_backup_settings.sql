-- Migration to add backup settings columns to user_settings table
-- Run this if upgrading from an older version

-- Add backup_auto_enabled column (SQLite uses INTEGER for boolean)
ALTER TABLE user_settings ADD COLUMN backup_auto_enabled INTEGER NOT NULL DEFAULT 0;

-- Add backup_cron column
ALTER TABLE user_settings ADD COLUMN backup_cron TEXT DEFAULT '0 0 2 * * *';

-- Add backup_retention_days column
ALTER TABLE user_settings ADD COLUMN backup_retention_days INTEGER NOT NULL DEFAULT 30;
