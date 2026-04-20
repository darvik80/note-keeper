package xyz.crearts.note.keeper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Telegram Bot API client for sending messages.
 * API: https://core.telegram.org/bots/api#sendmessage
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final RestClient restClient;

    public TelegramClient() {
        this.restClient = RestClient.create();
    }

    /**
     * Send text message to configured chat.
     * @param botToken Telegram bot token
     * @param chatId Chat ID to send message to
     * @param text message text
     * @return true if sent successfully
     */
    public boolean sendMessage(String botToken, String chatId, String text) {
        if (botToken == null || chatId == null) {
            log.warn("Telegram integration not configured (botToken={}, chatId={})", 
                    botToken != null, chatId != null);
            return false;
        }

        try {
            TelegramResponse response = restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                    .body(new TelegramMessage(chatId, text))
                    .retrieve()
                    .body(TelegramResponse.class);

            if (response != null && response.ok) {
                log.info("Telegram message sent successfully");
                return true;
            } else {
                log.error("Telegram API returned error: {}", response.getDescription());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            return false;
        }
    }

    private static class TelegramMessage {
        private final String chat_id;
        private final String text;

        public TelegramMessage(String chatId, String text) {
            this.chat_id = chatId;
            this.text = text;
        }

        public String getChat_id() {
            return chat_id;
        }

        public String getText() {
            return text;
        }
    }

    private static class TelegramResponse {
        private boolean ok;
        private String description;

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
