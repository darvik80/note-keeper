package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;

@Slf4j
@Service
public class IntegrationService {

    private final TelegramClient telegramClient;
    private final DingTalkClient dingTalkClient;

    public IntegrationService(TelegramClient telegramClient, DingTalkClient dingTalkClient) {
        this.telegramClient = telegramClient;
        this.dingTalkClient = dingTalkClient;
    }

    public IntegrationResponse sendToTelegram(IntegrationRequest request) {
        log.info("Telegram integration called with message: {}", request.getMessage());
        
        boolean success = telegramClient.sendMessage(
            request.getBotToken(),
            request.getChatId(),
            request.getMessage()
        );
        
        if (success) {
            return new IntegrationResponse(true, "Message sent to Telegram");
        } else {
            return new IntegrationResponse(false, "Failed to send message to Telegram (check configuration)");
        }
    }

    public IntegrationResponse sendToDingTalk(IntegrationRequest request) {
        log.info("DingTalk integration called with message: {}", request.getMessage());
        
        boolean success = dingTalkClient.sendMessage(
            request.getWebhook(),
            request.getSecret(),
            request.getMessage()
        );
        
        if (success) {
            return new IntegrationResponse(true, "Message sent to DingTalk");
        } else {
            return new IntegrationResponse(false, "Failed to send message to DingTalk (check configuration)");
        }
    }
}
