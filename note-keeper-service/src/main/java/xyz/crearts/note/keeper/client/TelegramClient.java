package xyz.crearts.note.keeper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telegram Bot API client for sending messages, inline keyboards, webhooks, and callbacks.
 * API: https://core.telegram.org/bots/api
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final RestClient restClient;

    public TelegramClient() {
        this.restClient = RestClient.create();
    }

    /**
     * Send plain text message (backward-compatible, no parse mode).
     */
    public boolean sendMessage(String botToken, String chatId, String text) {
        return sendMessage(botToken, chatId, text, null, null);
    }

    /**
     * Send message with optional MarkdownV2 parse mode and inline keyboard.
     *
     * @param botToken     Telegram bot token
     * @param chatId       Chat ID to send message to
     * @param text         message text
     * @param parseMode    parse mode (e.g. "MarkdownV2") or null for plain text
     * @param inlineKeyboard inline keyboard buttons (list of rows, each row is list of buttons) or null
     * @return true if sent successfully
     */
    public boolean sendMessage(String botToken, String chatId, String text,
                               String parseMode, List<List<InlineButton>> inlineKeyboard) {
        if (botToken == null || chatId == null) {
            log.warn("Telegram integration not configured (botToken={}, chatId={})",
                    botToken != null, chatId != null);
            return false;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            if (parseMode != null && !parseMode.isEmpty()) {
                body.put("parse_mode", parseMode);
            }
            if (inlineKeyboard != null && !inlineKeyboard.isEmpty()) {
                body.put("reply_markup", buildReplyMarkup(inlineKeyboard));
            }

            TelegramResponse response = restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);

            if (response != null && response.ok) {
                log.info("Telegram message sent successfully");
                return true;
            } else {
                log.error("Telegram API returned error: {}", response != null ? response.getDescription() : "null response");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            return false;
        }
    }

    /**
     * Register a webhook URL for receiving updates (callback queries, messages, etc.).
     *
     * @param botToken   Telegram bot token
     * @param webhookUrl publicly accessible URL to receive updates
     * @return true if webhook was set successfully
     */
    public boolean setWebhook(String botToken, String webhookUrl) {
        if (botToken == null || webhookUrl == null) {
            log.warn("Cannot set webhook: botToken or webhookUrl is null");
            return false;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("url", webhookUrl);
            body.put("allowed_updates", List.of("callback_query"));

            TelegramResponse response = restClient.post()
                    .uri("https://api.telegram.org/bot{token}/setWebhook", botToken)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);

            if (response != null && response.ok) {
                log.info("Telegram webhook set successfully: {}", webhookUrl);
                return true;
            } else {
                log.error("Failed to set Telegram webhook: {}", response != null ? response.getDescription() : "null response");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to set Telegram webhook", e);
            return false;
        }
    }

    /**
     * Answer a callback query to remove the loading animation on the button.
     *
     * @param botToken       Telegram bot token
     * @param callbackQueryId the callback query ID to answer
     * @param text           optional text to show in a notification
     * @return true if answered successfully
     */
    public boolean answerCallbackQuery(String botToken, String callbackQueryId, String text) {
        if (botToken == null || callbackQueryId == null) {
            return false;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("callback_query_id", callbackQueryId);
            if (text != null && !text.isEmpty()) {
                body.put("text", text);
                body.put("show_alert", false);
            }

            TelegramResponse response = restClient.post()
                    .uri("https://api.telegram.org/bot{token}/answerCallbackQuery", botToken)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);

            return response != null && response.ok;
        } catch (Exception e) {
            log.error("Failed to answer callback query", e);
            return false;
        }
    }

    /**
     * Edit a message text (e.g. to show "Done ✅" after marking a todo complete).
     *
     * @param botToken  Telegram bot token
     * @param chatId    Chat ID
     * @param messageId Message ID to edit
     * @param text      New text (MarkdownV2)
     * @return true if edited successfully
     */
    public boolean editMessageText(String botToken, String chatId, long messageId, String text) {
        if (botToken == null || chatId == null) {
            return false;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("message_id", messageId);
            body.put("text", text);
            body.put("parse_mode", "MarkdownV2");

            TelegramResponse response = restClient.post()
                    .uri("https://api.telegram.org/bot{token}/editMessageText", botToken)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);

            return response != null && response.ok;
        } catch (Exception e) {
            log.error("Failed to edit Telegram message", e);
            return false;
        }
    }

    /**
     * Remove inline keyboard from a message (after action is completed).
     */
    public boolean removeInlineKeyboard(String botToken, String chatId, long messageId) {
        if (botToken == null || chatId == null) {
            return false;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("message_id", messageId);
            body.put("reply_markup", Map.of());

            TelegramResponse response = restClient.post()
                    .uri("https://api.telegram.org/bot{token}/editMessageReplyMarkup", botToken)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);

            return response != null && response.ok;
        } catch (Exception e) {
            log.error("Failed to remove inline keyboard", e);
            return false;
        }
    }

    private Map<String, Object> buildReplyMarkup(List<List<InlineButton>> inlineKeyboard) {
        List<List<Map<String, String>>> rows = new ArrayList<>();
        for (List<InlineButton> row : inlineKeyboard) {
            List<Map<String, String>> buttons = new ArrayList<>();
            for (InlineButton btn : row) {
                Map<String, String> button = new LinkedHashMap<>();
                button.put("text", btn.text);
                button.put("callback_data", btn.callbackData);
                buttons.add(button);
            }
            rows.add(buttons);
        }
        return Map.of("inline_keyboard", rows);
    }

    /**
     * Represents an inline keyboard button.
     */
    public static class InlineButton {
        public final String text;
        public final String callbackData;

        public InlineButton(String text, String callbackData) {
            this.text = text;
            this.callbackData = callbackData;
        }
    }

    /** Telegram Bot API envelope — must be public for GraalVM / Jackson binding. */
    public static class TelegramResponse {
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
