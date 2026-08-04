package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;
import xyz.crearts.note.keeper.model.UserSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock private TelegramClient telegramClient;
    @Mock private DingTalkClient dingTalkClient;
    @Mock private UserSettingsService userSettingsService;

    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService(telegramClient, dingTalkClient, userSettingsService);
    }

    @Test
    void sendToTelegram_success_shouldReturnSuccessResponse() {
        IntegrationRequest request = new IntegrationRequest();
        request.setBotToken("bot-token");
        request.setChatId("chat-id");
        request.setMessage("Hello Telegram");

        when(telegramClient.sendMessage("bot-token", "chat-id", "Hello Telegram")).thenReturn(true);

        IntegrationResponse response = integrationService.sendToTelegram(request);

        assertTrue(response.isSuccess());
        verify(telegramClient).sendMessage("bot-token", "chat-id", "Hello Telegram");
    }

    @Test
    void sendToTelegram_failure_shouldReturnFailureResponse() {
        IntegrationRequest request = new IntegrationRequest();
        request.setBotToken("bad-token");
        request.setChatId("chat-id");
        request.setMessage("Hello");

        when(telegramClient.sendMessage("bad-token", "chat-id", "Hello")).thenReturn(false);

        IntegrationResponse response = integrationService.sendToTelegram(request);

        assertFalse(response.isSuccess());
    }

    @Test
    void sendToDingTalk_success_shouldReturnSuccessResponse() {
        IntegrationRequest request = new IntegrationRequest();
        request.setWebhook("https://oapi.dingtalk.com/robot/send?access_token=xxx");
        request.setSecret("secret");
        request.setMessage("Hello DingTalk");

        when(dingTalkClient.sendMessage(
                "https://oapi.dingtalk.com/robot/send?access_token=xxx",
                "secret", "Hello DingTalk")).thenReturn(true);

        IntegrationResponse response = integrationService.sendToDingTalk(request);

        assertTrue(response.isSuccess());
        verify(dingTalkClient).sendMessage(
                "https://oapi.dingtalk.com/robot/send?access_token=xxx",
                "secret", "Hello DingTalk");
    }

    @Test
    void sendToDingTalk_failure_shouldReturnFailureResponse() {
        IntegrationRequest request = new IntegrationRequest();
        request.setWebhook("webhook");
        request.setSecret("secret");
        request.setMessage("Hello");

        when(dingTalkClient.sendMessage("webhook", "secret", "Hello")).thenReturn(false);

        IntegrationResponse response = integrationService.sendToDingTalk(request);

        assertFalse(response.isSuccess());
    }

    @Test
    void sendTestTodoToTelegram_noSettings_shouldReturnFailure() {
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(null);

        IntegrationResponse response = integrationService.sendTestTodoToTelegram("user-1");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not configured"));
    }

    @Test
    void sendTestTodoToTelegram_noBotToken_shouldReturnFailure() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramChatId("chat-123");
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(settings);

        IntegrationResponse response = integrationService.sendTestTodoToTelegram("user-1");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not configured"));
    }

    @Test
    void sendTestTodoToTelegram_success_shouldSendMarkdownV2WithKeyboard() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramBotToken("bot-token");
        settings.setTelegramChatId("chat-123");
        settings.setTelegramWebhookSecret("webhook-secret");
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(settings);

        when(telegramClient.sendMessage(eq("bot-token"), eq("chat-123"), anyString(), eq("MarkdownV2"), anyList()))
                .thenReturn(true);

        IntegrationResponse response = integrationService.sendTestTodoToTelegram("user-1");

        assertTrue(response.isSuccess());
        assertEquals("Test todo sent to Telegram", response.getMessage());
        verify(telegramClient).sendMessage(eq("bot-token"), eq("chat-123"), anyString(), eq("MarkdownV2"), anyList());
    }

    @Test
    void sendTestTodoToTelegram_noWebhookSecret_shouldSendWithoutKeyboard() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramBotToken("bot-token");
        settings.setTelegramChatId("chat-123");
        // No webhook secret set
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(settings);

        // No webhook base URL configured, so ensureWebhookRegistered is a no-op
        ReflectionTestUtils.setField(integrationService, "webhookBaseUrl", "");

        when(telegramClient.sendMessage(eq("bot-token"), eq("chat-123"), anyString(), eq("MarkdownV2"), isNull()))
                .thenReturn(true);

        IntegrationResponse response = integrationService.sendTestTodoToTelegram("user-1");

        assertTrue(response.isSuccess());
        // Verify sent without keyboard (null)
        verify(telegramClient).sendMessage(eq("bot-token"), eq("chat-123"), anyString(), eq("MarkdownV2"), isNull());
    }

    @Test
    void sendTestTodoToTelegram_markdownV2Fails_shouldFallbackToPlainText() {
        UserSettings settings = new UserSettings();
        settings.setId("user-1");
        settings.setTelegramBotToken("bot-token");
        settings.setTelegramChatId("chat-123");
        settings.setTelegramWebhookSecret("webhook-secret");
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(settings);

        // MarkdownV2 fails
        when(telegramClient.sendMessage(eq("bot-token"), eq("chat-123"), anyString(), eq("MarkdownV2"), anyList()))
                .thenReturn(false);
        // Plain text fallback succeeds
        when(telegramClient.sendMessage(eq("bot-token"), eq("chat-123"), anyString()))
                .thenReturn(true);

        IntegrationResponse response = integrationService.sendTestTodoToTelegram("user-1");

        assertTrue(response.isSuccess());
        verify(telegramClient).sendMessage(eq("bot-token"), eq("chat-123"), anyString(), eq("MarkdownV2"), anyList());
        verify(telegramClient).sendMessage(eq("bot-token"), eq("chat-123"), anyString());
    }
}
