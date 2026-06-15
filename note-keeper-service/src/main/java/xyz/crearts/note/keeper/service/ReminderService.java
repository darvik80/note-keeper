package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling Todo reminders and sending notifications.
 * Checks for due reminders every minute and sends notifications via Telegram/DingTalk.
 */
@Slf4j
@Service
public class ReminderService {

    private final TodoMapper todoMapper;
    private final TelegramClient telegramClient;
    private final DingTalkClient dingTalkClient;
    private final UserSettingsService userSettingsService;

    public ReminderService(TodoMapper todoMapper, TelegramClient telegramClient, DingTalkClient dingTalkClient, UserSettingsService userSettingsService) {
        this.todoMapper = todoMapper;
        this.telegramClient = telegramClient;
        this.dingTalkClient = dingTalkClient;
        this.userSettingsService = userSettingsService;
    }

    /**
     * Check for due reminders every minute.
     * Sends notifications for todos with reminder time in the past that haven't been notified yet.
     */
    @Scheduled(fixedRate = 60000) // every 60 seconds
    public void checkReminders() {
        log.debug("Checking for due reminders...");
        
        LocalDateTime now = LocalDateTime.now();
        // Find todos with reminder time <= now
        List<Todo> todosWithReminders = todoMapper.findWithDueReminders(now);
        
        for (Todo todo : todosWithReminders) {
            if (todo.getReminder() != null && !todo.isDeleted() && !todo.isArchived()) {
                sendReminderNotification(todo);
            }
        }
    }

    /**
     * Send reminder notification for a todo.
     * Sends to selected channels (Telegram, DingTalk, or both).
     */
    private void sendReminderNotification(Todo todo) {
        String message = buildReminderMessage(todo);
        
        log.info("Sending reminder for todo: {} - {}", todo.getId(), todo.getTitle());
        
        // Get user credentials from settings (in production, load from database)
        // For now, using placeholder - credentials will come from UI settings
        
        String channels = todo.getNotificationChannels();
        if (channels == null || channels.isEmpty()) {
            // Default to both channels if not specified
            channels = "telegram,dingtalk";
        }
        
        String[] channelArray = channels.split(",");
        for (String channel : channelArray) {
            String trimmedChannel = channel.trim();
            if ("telegram".equalsIgnoreCase(trimmedChannel)) {
                sendToTelegram(todo, message);
            } else if ("dingtalk".equalsIgnoreCase(trimmedChannel)) {
                sendToDingTalk(todo, message);
            }
        }
        
        // Mark reminder as notified
        todoMapper.markReminderNotified(todo.getId(), LocalDateTime.now());
    }

    /**
     * Send notification to Telegram.
     * Gets credentials from user_settings table.
     */
    private void sendToTelegram(Todo todo, String message) {
        String userId = todo.getOwnerId() != null ? todo.getOwnerId() : "default";
        UserSettings settings = userSettingsService.getDecryptedSettings(userId);
        if (settings == null || settings.getTelegramBotToken() == null || settings.getTelegramChatId() == null) {
            log.warn("Telegram credentials not configured. Skipping notification for todo: {}", todo.getId());
            return;
        }
        
        log.info("Sending Telegram notification for todo: {}", todo.getId());
        boolean success = telegramClient.sendMessage(
            settings.getTelegramBotToken(),
            settings.getTelegramChatId(),
            message
        );
        
        if (success) {
            log.info("Telegram notification sent successfully for todo: {}", todo.getId());
        } else {
            log.error("Failed to send Telegram notification for todo: {}", todo.getId());
        }
    }

    /**
     * Send notification to DingTalk.
     * Gets credentials from user_settings table.
     */
    private void sendToDingTalk(Todo todo, String message) {
        String userId = todo.getOwnerId() != null ? todo.getOwnerId() : "default";
        UserSettings settings = userSettingsService.getDecryptedSettings(userId);
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
     * Build reminder message for notification.
     */
    private String buildReminderMessage(Todo todo) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ Reminder: ").append(todo.getTitle());
        
        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            sb.append("\n").append(todo.getDescription().substring(0, Math.min(100, todo.getDescription().length())));
        }
        
        if (todo.getDueDate() != null) {
            sb.append("\n📅 Due: ").append(todo.getDueDate());
        }
        
        if (todo.getPriority() != null) {
            sb.append("\nPriority: ").append(todo.getPriority());
        }
        
        return sb.toString();
    }
}
