-- Add telegram_webhook_secret column to user_settings for Telegram inline keyboard callbacks
ALTER TABLE user_settings ADD COLUMN telegram_webhook_secret TEXT;
