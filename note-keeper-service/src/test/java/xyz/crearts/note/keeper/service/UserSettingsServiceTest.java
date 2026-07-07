package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.mapper.UserSettingsMapper;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock private UserSettingsMapper userSettingsMapper;
    @Mock private EncryptionService encryptionService;

    private UserSettingsService userSettingsService;

    @BeforeEach
    void setUp() {
        userSettingsService = new UserSettingsService(userSettingsMapper, encryptionService);
    }

    @Test
    void getSettings_existingSettings_shouldDecryptAndReturn() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramBotToken("encrypted-token");
        settings.setTelegramChatId("encrypted-chat-id");
        settings.setDingtalkWebhook("encrypted-webhook");
        settings.setDingtalkSecret("encrypted-secret");

        when(userSettingsMapper.findById("user-1")).thenReturn(settings);
        when(encryptionService.decrypt("encrypted-token")).thenReturn("bot-token-plain");
        when(encryptionService.decrypt("encrypted-chat-id")).thenReturn("chat-id-plain");
        when(encryptionService.decrypt("encrypted-webhook")).thenReturn("webhook-plain");
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("secret-plain");

        UserSettings result = userSettingsService.getSettings("user-1");

        assertNotNull(result);
        assertEquals("bot-token-plain", result.getTelegramBotToken());
        assertEquals("chat-id-plain", result.getTelegramChatId());
        assertEquals("webhook-plain", result.getDingtalkWebhook());
        assertEquals("secret-plain", result.getDingtalkSecret());
    }

    @Test
    void getSettings_nonExistent_shouldReturnNull() {
        when(userSettingsMapper.findById("missing")).thenReturn(null);

        UserSettings result = userSettingsService.getSettings("missing");

        assertNull(result);
    }

    @Test
    void saveSettings_shouldEncryptAndUpsert() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramBotToken("plain-token");
        settings.setTelegramChatId("plain-chat");
        settings.setDingtalkWebhook("plain-webhook");
        settings.setDingtalkSecret("plain-secret");

        when(encryptionService.encrypt("plain-token")).thenReturn("encrypted-token");
        when(encryptionService.encrypt("plain-chat")).thenReturn("encrypted-chat");
        when(encryptionService.encrypt("plain-webhook")).thenReturn("encrypted-webhook");
        when(encryptionService.encrypt("plain-secret")).thenReturn("encrypted-secret");

        userSettingsService.saveSettings(settings);

        verify(userSettingsMapper).upsert(argThat(s ->
                "encrypted-token".equals(s.getTelegramBotToken()) &&
                "encrypted-chat".equals(s.getTelegramChatId()) &&
                "encrypted-webhook".equals(s.getDingtalkWebhook()) &&
                "encrypted-secret".equals(s.getDingtalkSecret())));
    }

    @Test
    void saveSettings_nullFields_shouldNotEncrypt() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        // All fields null

        userSettingsService.saveSettings(settings);

        verify(encryptionService, never()).encrypt(anyString());
        verify(userSettingsMapper).upsert(any(UserSettings.class));
    }

    @Test
    void getSettings_decryptionFailure_shouldReturnOriginalValue() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramBotToken("legacy-unencrypted-value");

        when(userSettingsMapper.findById("user-1")).thenReturn(settings);
        when(encryptionService.decrypt("legacy-unencrypted-value")).thenThrow(new RuntimeException("decrypt failed"));

        UserSettings result = userSettingsService.getSettings("user-1");

        assertNotNull(result);
        // Should return original value on decryption failure (legacy data)
        assertEquals("legacy-unencrypted-value", result.getTelegramBotToken());
    }

    @Test
    void getDecryptedSettings_shouldReturnSameAsGetSettings() {
        when(userSettingsMapper.findById("user-1")).thenReturn(null);

        UserSettings result = userSettingsService.getDecryptedSettings("user-1");

        assertNull(result);
    }
}
