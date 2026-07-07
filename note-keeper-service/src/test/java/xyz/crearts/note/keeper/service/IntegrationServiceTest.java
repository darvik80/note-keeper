package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

    @Mock private TelegramClient telegramClient;
    @Mock private DingTalkClient dingTalkClient;

    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService(telegramClient, dingTalkClient);
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
}
