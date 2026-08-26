package xyz.crearts.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.client.TelegramMarkdownUtil;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Todo reminder scheduler.
 * Every minute: rollover recurring periods, then notify due incomplete reminders.
 * Telegram notifications use MarkdownV2 formatting with inline keyboard for quick actions.
 * Notes have a reminder field but are NOT handled here (display-only).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TodoMapper todoMapper;
    private final TelegramClient telegramClient;
    private final DingTalkClient dingTalkClient;
    private final UserSettingsService userSettingsService;

    @Value("${app.telegram.webhook-base-url:}")
    private String webhookBaseUrl;

    /**
     * Every minute: rollover recurring periods, then notify due incomplete reminders.
     * Notify does not advance reminder — next fire happens after rollover into a new slot.
     */
    @Scheduled(fixedRate = 60000)
    public void checkReminders() {
        log.debug("Checking for due reminders...");
        LocalDateTime now = Recurrence.nowUtc();

        for (Todo todo : todoMapper.findRecurringActive(null)) {
            if (Recurrence.rolloverIfNeeded(todo, now)) {
                todoMapper.update(todo);
                log.info("Rolled recurring todo {} to reminder={} completed={}",
                        todo.getId(), todo.getReminder(), todo.isCompleted());
            }
        }

        for (Todo todo : todoMapper.findWithDueReminders(now)) {
            if (todo.getReminder() != null && !todo.isDeleted() && !todo.isArchived() && !todo.isCompleted()) {
                sendReminderNotification(todo);
            }
        }
    }

    private void sendReminderNotification(Todo todo) {
        log.info("Sending reminder for todo: {} - {}", todo.getId(), todo.getTitle());
        dispatchChannels(todo, buildReminderMessageMarkdownV2(todo), buildReminderMessagePlain(todo));

        LocalDateTime notifiedAt = Recurrence.nowUtc();
        todoMapper.markReminderNotified(todo.getId(), notifiedAt);
        todo.setNotifiedAt(notifiedAt);
    }

    static boolean isRecurring(Todo todo) {
        return Recurrence.isRecurring(todo);
    }

    static LocalDateTime advance(LocalDateTime from, String repeat) {
        return Recurrence.advance(from, repeat);
    }

    static LocalDateTime advanceDueDate(LocalDateTime dueDate, LocalDateTime oldReminder, LocalDateTime newReminder) {
        return Recurrence.advanceDueDate(dueDate, oldReminder, newReminder);
    }

    private void dispatchChannels(Todo todo, String markdownMessage, String plainMessage) {
        String channels = todo.getNotificationChannels();
        if (channels == null || channels.isBlank()) {
            channels = "telegram";
        }
        for (String channel : channels.split(",")) {
            switch (channel.trim().toLowerCase()) {
                case "telegram" -> sendToTelegram(todo, markdownMessage, plainMessage);
                case "dingtalk" -> sendToDingTalk(todo, plainMessage);
                default -> log.warn("Unknown notification channel '{}' for todo {}", channel, todo.getId());
            }
        }
    }

    private void sendToTelegram(Todo todo, String markdownMessage, String plainMessage) {
        String userId = todo.getOwnerId() != null ? todo.getOwnerId() : "default";
        UserSettings settings = userSettingsService.getSettings(userId);
        if (settings == null || settings.getTelegramBotToken() == null || settings.getTelegramChatId() == null) {
            log.warn("Telegram credentials not configured. Skipping notification for todo: {}", todo.getId());
            return;
        }

        // Ensure webhook is set for inline keyboard callbacks
        ensureWebhookRegistered(settings);

        // Build inline keyboard with "Done" button
        List<List<TelegramClient.InlineButton>> keyboard = null;
        if (settings.getTelegramWebhookSecret() != null && !settings.getTelegramWebhookSecret().isEmpty()) {
            String callbackData = "done:" + todo.getId();
            // Telegram callback_data max 64 bytes
            if (callbackData.length() <= 64) {
                keyboard = List.of(
                        List.of(new TelegramClient.InlineButton("✅ Mark as Done", callbackData))
                );
            }
        }

        log.info("Sending Telegram notification for todo: {}", todo.getId());
        boolean success = telegramClient.sendMessage(
                settings.getTelegramBotToken(),
                settings.getTelegramChatId(),
                markdownMessage,
                "MarkdownV2",
                keyboard
        );

        // Fallback: if MarkdownV2 fails, try plain text
        if (!success) {
            log.warn("MarkdownV2 message failed, falling back to plain text for todo: {}", todo.getId());
            success = telegramClient.sendMessage(
                    settings.getTelegramBotToken(),
                    settings.getTelegramChatId(),
                    plainMessage
            );
        }

        if (success) {
            log.info("Telegram notification sent successfully for todo: {}", todo.getId());
        } else {
            log.error("Failed to send Telegram notification for todo: {}", todo.getId());
        }
    }

    /**
     * Register the Telegram webhook if webhook-base-url is configured and user has a webhook secret.
     * Generates a webhook secret if the user doesn't have one yet.
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

    private void sendToDingTalk(Todo todo, String message) {
        String userId = todo.getOwnerId() != null ? todo.getOwnerId() : "default";
        UserSettings settings = userSettingsService.getSettings(userId);
        if (settings == null || settings.getDingtalkWebhook() == null || settings.getDingtalkSecret() == null) {
            log.warn("DingTalk credentials not configured. Skipping notification for todo: {}", todo.getId());
            return;
        }

        log.info("Sending DingTalk notification for todo: {}", todo.getId());
        boolean success = dingTalkClient.sendMessage(
                settings.getDingtalkWebhook(),
                settings.getDingtalkSecret(),
                message
        );

        if (success) {
            log.info("DingTalk notification sent successfully for todo: {}", todo.getId());
        } else {
            log.error("Failed to send DingTalk notification for todo: {}", todo.getId());
        }
    }

    /**
     * Build a MarkdownV2 formatted reminder message for Telegram.
     * Uses bold text, proper formatting, and escaped special characters.
     */
    String buildReminderMessageMarkdownV2(Todo todo) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ ").append(TelegramMarkdownUtil.bold("Reminder")).append("\n\n");
        sb.append("📋 ").append(TelegramMarkdownUtil.bold(todo.getTitle()));

        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            String desc = todo.getDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 197) + "\\.\\.\\.";
            }
            sb.append("\n").append(TelegramMarkdownUtil.escapeMarkdownV2(desc));
        }

        if (todo.getDueDate() != null) {
            sb.append("\n\n").append(TelegramMarkdownUtil.field("📅", "Due:", todo.getDueDate().format(DATE_TIME_FMT)));
        }

        if (todo.getPriority() != null) {
            String priorityIcon = switch (todo.getPriority()) {
                case "high" -> "🔴";
                case "medium" -> "🟡";
                case "low" -> "🟢";
                default -> "⚪";
            };
            sb.append("\n").append(priorityIcon).append(" ")
                    .append(TelegramMarkdownUtil.field("", "Priority:", todo.getPriority()));
        }

        if (isRecurring(todo)) {
            sb.append("\n").append(TelegramMarkdownUtil.field("🔁", "Repeat:", todo.getSchedule().getRepeat()));
        }

        return sb.toString();
    }

    /**
     * Build a plain text reminder message (fallback for DingTalk or if MarkdownV2 fails).
     */
    private String buildReminderMessagePlain(Todo todo) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ Reminder: ").append(todo.getTitle());

        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            sb.append("\n").append(todo.getDescription(), 0, Math.min(100, todo.getDescription().length()));
        }

        if (todo.getDueDate() != null) {
            sb.append("\n📅 Due: ").append(todo.getDueDate().format(DATE_TIME_FMT));
        }

        if (todo.getPriority() != null) {
            sb.append("\nPriority: ").append(todo.getPriority());
        }

        if (isRecurring(todo)) {
            sb.append("\n🔁 Repeats: ").append(todo.getSchedule().getRepeat());
        }

        return sb.toString();
    }

}
