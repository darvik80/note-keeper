package xyz.crearts.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.service.TelegramWebhookService;

import java.util.Map;

/**
 * Receives Telegram webhook updates (callback queries from inline keyboards).
 * The webhook URL is: /api/v1/telegram/webhook/{webhookSecret}
 * Each user has a unique webhookSecret to identify them.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/telegram/webhook")
@Tag(name = "Telegram Webhook")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramWebhookService telegramWebhookService;

    /**
     * Handle incoming Telegram webhook updates.
     * We only process callback_query updates (from inline keyboard buttons).
     */
    @PostMapping("/{secret}")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(
            @PathVariable String secret,
            @RequestBody Map<String, Object> update) {

        log.debug("Received Telegram webhook update for secret: {}", secret);

        @SuppressWarnings("unchecked")
        Map<String, Object> callbackQuery = (Map<String, Object>) update.get("callback_query");
        if (callbackQuery == null) {
            // Not a callback query, ignore (e.g. message, edited_message)
            return ResponseEntity.ok(Map.of("ok", true));
        }

        String callbackQueryId = (String) callbackQuery.get("id");
        String callbackData = (String) callbackQuery.get("data");

        // Extract chat_id and message_id from the message
        String chatId = null;
        Long messageId = null;

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) callbackQuery.get("message");
        if (message != null) {
            Object chatObj = message.get("chat");
            if (chatObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> chat = (Map<String, Object>) chatObj;
                Object id = chat.get("id");
                chatId = id != null ? id.toString() : null;
            }
            Object msgId = message.get("message_id");
            if (msgId instanceof Number) {
                messageId = ((Number) msgId).longValue();
            }
        }

        boolean processed = telegramWebhookService.processCallbackQuery(
                secret, callbackQueryId, callbackData, chatId, messageId);

        return ResponseEntity.ok(Map.of("ok", processed));
    }
}
