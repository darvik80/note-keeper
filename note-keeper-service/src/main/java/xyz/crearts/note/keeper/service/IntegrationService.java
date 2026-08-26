package xyz.crearts.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.client.TelegramMarkdownUtil;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;
import xyz.crearts.note.keeper.model.UserSettings;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final TelegramClient telegramClient;
    private final DingTalkClient dingTalkClient;
    private final UserSettingsService userSettingsService;

    @Value("${app.telegram.webhook-base-url:}")
    private String webhookBaseUrl;

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

    /**
     * Send a test todo reminder to the current user's Telegram chat.
     * Uses stored credentials and registers webhook if needed.
     * Sends a MarkdownV2 formatted message with inline "Mark as Done" keyboard.
     *
     * @param userId authenticated user ID from JWT
     */
    public IntegrationResponse sendTestTodoToTelegram(String userId) {
        UserSettings settings = userSettingsService.getSettings(userId);
        if (settings == null || settings.getTelegramBotToken() == null || settings.getTelegramChatId() == null) {
            return new IntegrationResponse(false, "Telegram credentials not configured. Save your bot token and chat ID first.");
        }

        String botToken = settings.getTelegramBotToken();
        String chatId = settings.getTelegramChatId();

        try {
            // Build MarkdownV2 test message
            String markdownMessage = buildTestTodoMarkdownV2();
            String plainMessage = "🧪 Test Todo from NoteKeeper\n\nThis is a test todo reminder with a 'Mark as Done' button.";

            // Ensure webhook is registered for inline keyboard callbacks
            ensureWebhookRegistered(settings);

            // Build inline keyboard with "Done" button
            List<List<TelegramClient.InlineButton>> keyboard = null;
            if (settings.getTelegramWebhookSecret() != null && !settings.getTelegramWebhookSecret().isEmpty()) {
                String callbackData = "done:test";
                keyboard = List.of(
                        List.of(new TelegramClient.InlineButton("✅ Mark as Done", callbackData))
                );
            }

            log.info("Sending test todo notification to Telegram for user: {}", userId);
            boolean success = telegramClient.sendMessage(botToken, chatId, markdownMessage, "MarkdownV2", keyboard);

            // Fallback: if MarkdownV2 fails, try plain text
            if (!success) {
                log.warn("MarkdownV2 test message failed, falling back to plain text");
                success = telegramClient.sendMessage(botToken, chatId, plainMessage);
            }

            if (success) {
                return new IntegrationResponse(true, "Test todo sent to Telegram");
            } else {
                return new IntegrationResponse(false, "Failed to send test todo to Telegram (check configuration)");
            }
        } catch (Exception e) {
            log.error("Failed to send test todo to Telegram", e);
            return new IntegrationResponse(false, "Failed to send test todo: " + e.getMessage());
        }
    }

    /**
     * Build a MarkdownV2 formatted test todo message for Telegram.
     */
    private String buildTestTodoMarkdownV2() {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ ").append(TelegramMarkdownUtil.bold("Reminder")).append("\n\n");
        sb.append("📋 ").append(TelegramMarkdownUtil.bold("Test Todo from NoteKeeper"));
        sb.append("\n").append(TelegramMarkdownUtil.escapeMarkdownV2("This is a test todo reminder with a 'Mark as Done' button."));
        sb.append("\n\n").append(TelegramMarkdownUtil.field("📅", "Due:", "04 Aug 2026, 12:00"));
        sb.append("\n").append(TelegramMarkdownUtil.field("🟡", "Priority:", "medium"));
        return sb.toString();
    }

    /**
     * Register the Telegram webhook if webhook-base-url is configured.
     * Generates and persists a webhook secret if the user doesn't have one yet.
     */
    private void ensureWebhookRegistered(UserSettings settings) {
        if (webhookBaseUrl == null || webhookBaseUrl.isBlank()) {
            return;
        }

        // Generate webhook secret if not set
        if (settings.getTelegramWebhookSecret() == null || settings.getTelegramWebhookSecret().isEmpty()) {
            String secret = UUID.randomUUID().toString().replace("-", "");
            settings.setTelegramWebhookSecret(secret);

            // Persist the secret (encrypt before storing)
            UserSettings toSave = new UserSettings();
            toSave.setId(settings.getId());
            toSave.setTelegramBotToken(settings.getTelegramBotToken());
            toSave.setTelegramChatId(settings.getTelegramChatId());
            toSave.setTelegramWebhookSecret(secret);
            toSave.setDingtalkWebhook(settings.getDingtalkWebhook());
            toSave.setDingtalkSecret(settings.getDingtalkSecret());
            userSettingsService.saveSettings(toSave);

            log.info("Generated Telegram webhook secret for user: {}", settings.getId());
        }

        String webhookUrl = webhookBaseUrl.replaceAll("/+$", "")
                + "/api/v1/telegram/webhook/" + settings.getTelegramWebhookSecret();

        telegramClient.setWebhook(settings.getTelegramBotToken(), webhookUrl);
    }
}
