package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.mapper.UserSettingsMapper;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDateTime;

/**
 * Service for managing user settings with encryption of sensitive fields.
 * Encrypts Telegram/DingTalk credentials before storing in DB,
 * decrypts when reading back.
 */
@Slf4j
@Service
public class UserSettingsService {

    private final UserSettingsMapper userSettingsMapper;
    private final EncryptionService encryptionService;

    public UserSettingsService(UserSettingsMapper userSettingsMapper, EncryptionService encryptionService) {
        this.userSettingsMapper = userSettingsMapper;
        this.encryptionService = encryptionService;
    }

    /**
     * Get user settings with decrypted sensitive fields.
     * Also migrates telegramWebhookSecret from encrypted to plain text if needed.
     */
    public UserSettings getSettings(String userId) {
        UserSettings settings = userSettingsMapper.findById(userId);
        if (settings == null) {
            return null;
        }
        UserSettings decrypted = decryptSettings(settings);
        migrateWebhookSecretIfNeeded(settings, decrypted);
        return decrypted;
    }

    /**
     * Save user settings with encrypted sensitive fields.
     */
    public void saveSettings(UserSettings settings) {
        UserSettings encrypted = encryptSettings(settings);
        encrypted.setUpdatedAt(LocalDateTime.now());
        userSettingsMapper.upsert(encrypted);
        log.info("User settings saved for user: {}", settings.getId());
    }

    /**
     * Get decrypted settings for internal use (e.g., ReminderService).
     */
    public UserSettings getDecryptedSettings(String userId) {
        return getSettings(userId);
    }

    /**
     * Find user settings by Telegram webhook secret.
     * The webhook secret is stored as plain text (it's a public token in the webhook URL).
     */
    public UserSettings findByTelegramWebhookSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return null;
        }
        UserSettings settings = userSettingsMapper.findByTelegramWebhookSecret(secret);
        if (settings == null) {
            return null;
        }
        return decryptSettings(settings);
    }

    private UserSettings encryptSettings(UserSettings settings) {
        UserSettings encrypted = new UserSettings();
        encrypted.setId(settings.getId());
        encrypted.setTelegramBotToken(encryptSafe(settings.getTelegramBotToken()));
        encrypted.setTelegramChatId(encryptSafe(settings.getTelegramChatId()));
        // Webhook secret is NOT encrypted — it's a public token in the webhook URL
        encrypted.setTelegramWebhookSecret(settings.getTelegramWebhookSecret());
        encrypted.setDingtalkWebhook(encryptSafe(settings.getDingtalkWebhook()));
        encrypted.setDingtalkSecret(encryptSafe(settings.getDingtalkSecret()));
        // Daily report fields are not sensitive — copy as-is
        encrypted.setDailyReportEnabled(settings.isDailyReportEnabled());
        encrypted.setDailyReportTime(settings.getDailyReportTime());
        encrypted.setDailyReportChannels(settings.getDailyReportChannels());
        encrypted.setDailyReportTemplate(settings.getDailyReportTemplate());
        encrypted.setDailyReportItemTemplate(settings.getDailyReportItemTemplate());
        encrypted.setDailyReportLastSent(settings.getDailyReportLastSent());
        encrypted.setUpdatedAt(settings.getUpdatedAt());
        return encrypted;
    }

    private UserSettings decryptSettings(UserSettings settings) {
        UserSettings decrypted = new UserSettings();
        decrypted.setId(settings.getId());
        decrypted.setTelegramBotToken(decryptSafe(settings.getTelegramBotToken()));
        decrypted.setTelegramChatId(decryptSafe(settings.getTelegramChatId()));
        // Webhook secret is stored as plain text, no decryption needed
        decrypted.setTelegramWebhookSecret(settings.getTelegramWebhookSecret());
        decrypted.setDingtalkWebhook(decryptSafe(settings.getDingtalkWebhook()));
        decrypted.setDingtalkSecret(decryptSafe(settings.getDingtalkSecret()));
        // Daily report fields are not sensitive — copy as-is
        decrypted.setDailyReportEnabled(settings.isDailyReportEnabled());
        decrypted.setDailyReportTime(settings.getDailyReportTime());
        decrypted.setDailyReportChannels(settings.getDailyReportChannels());
        decrypted.setDailyReportTemplate(settings.getDailyReportTemplate());
        decrypted.setDailyReportItemTemplate(settings.getDailyReportItemTemplate());
        decrypted.setDailyReportLastSent(settings.getDailyReportLastSent());
        decrypted.setUpdatedAt(settings.getUpdatedAt());
        return decrypted;
    }

    /**
     * Migration: if the webhook secret is still encrypted in the DB (from before the fix),
     * decrypt it and re-save as plain text so SQL lookup works.
     */
    private void migrateWebhookSecretIfNeeded(UserSettings raw, UserSettings decrypted) {
        String rawSecret = raw.getTelegramWebhookSecret();
        if (rawSecret == null || rawSecret.isEmpty()) {
            return;
        }
        // Try to decrypt — if it succeeds, the value was still encrypted (legacy)
        try {
            String decryptedSecret = encryptionService.decrypt(rawSecret);
            // It was encrypted — re-save as plain text
            raw.setTelegramWebhookSecret(decryptedSecret);
            userSettingsMapper.upsert(raw);
            decrypted.setTelegramWebhookSecret(decryptedSecret);
            log.info("Migrated telegramWebhookSecret from encrypted to plain text for user: {}", raw.getId());
        } catch (Exception e) {
            // Already plain text — nothing to migrate
        }
    }

    private String encryptSafe(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            return encryptionService.encrypt(value);
        } catch (Exception e) {
            log.error("Failed to encrypt settings field", e);
            return value;
        }
    }

    private String decryptSafe(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            return encryptionService.decrypt(value);
        } catch (Exception e) {
            // Value might not be encrypted yet (migration case)
            log.debug("Failed to decrypt settings field, returning as-is (may be unencrypted legacy data)");
            return value;
        }
    }
}
