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
     */
    public UserSettings getSettings(String userId) {
        UserSettings settings = userSettingsMapper.findById(userId);
        if (settings == null) {
            return null;
        }
        return decryptSettings(settings);
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

    private UserSettings encryptSettings(UserSettings settings) {
        UserSettings encrypted = new UserSettings();
        encrypted.setId(settings.getId());
        encrypted.setTelegramBotToken(encryptSafe(settings.getTelegramBotToken()));
        encrypted.setTelegramChatId(encryptSafe(settings.getTelegramChatId()));
        encrypted.setDingtalkWebhook(encryptSafe(settings.getDingtalkWebhook()));
        encrypted.setDingtalkSecret(encryptSafe(settings.getDingtalkSecret()));
        encrypted.setUpdatedAt(settings.getUpdatedAt());
        return encrypted;
    }

    private UserSettings decryptSettings(UserSettings settings) {
        UserSettings decrypted = new UserSettings();
        decrypted.setId(settings.getId());
        decrypted.setTelegramBotToken(decryptSafe(settings.getTelegramBotToken()));
        decrypted.setTelegramChatId(decryptSafe(settings.getTelegramChatId()));
        decrypted.setDingtalkWebhook(decryptSafe(settings.getDingtalkWebhook()));
        decrypted.setDingtalkSecret(decryptSafe(settings.getDingtalkSecret()));
        decrypted.setUpdatedAt(settings.getUpdatedAt());
        return decrypted;
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
