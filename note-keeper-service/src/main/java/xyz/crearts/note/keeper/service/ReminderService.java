package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Todo reminder scheduler.
 * Every minute: notify due reminders via Telegram/DingTalk, then advance recurring schedules.
 * Notes have a reminder field but are NOT handled here (display-only).
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
     * Also catches up recurring todos whose reminder was never advanced after the first notify.
     */
    @Scheduled(fixedRate = 60000)
    public void checkReminders() {
        log.debug("Checking for due reminders...");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (Todo todo : todoMapper.findWithDueReminders(now)) {
            if (todo.getReminder() != null && !todo.isDeleted() && !todo.isArchived()) {
                sendReminderNotification(todo, now);
            }
        }

        for (Todo todo : todoMapper.findStuckRecurringReminders(now)) {
            catchUpStuckRecurring(todo, now);
        }
    }

    private void sendReminderNotification(Todo todo, LocalDateTime now) {
        log.info("Sending reminder for todo: {} - {}", todo.getId(), todo.getTitle());
        dispatchChannels(todo, buildReminderMessage(todo));

        LocalDateTime notifiedAt = LocalDateTime.now(ZoneOffset.UTC);
        todoMapper.markReminderNotified(todo.getId(), notifiedAt);
        todo.setNotifiedAt(notifiedAt);

        advanceRecurringIfNeeded(todo, now);
    }

    /**
     * Stuck recurring: reminder still in the past after notify.
     * Notify once for the most recent missed occurrence, then jump reminder to next future slot.
     */
    private void catchUpStuckRecurring(Todo todo, LocalDateTime now) {
        if (!isRecurring(todo) || todo.getReminder() == null) {
            return;
        }

        String repeat = todo.getSchedule().getRepeat();
        LocalDateTime endDate = todo.getSchedule().getEndDate();

        LocalDateTime lastOccurrence = todo.getReminder();
        LocalDateTime next = advance(lastOccurrence, repeat);

        while (next != null && !next.isAfter(now)) {
            if (endDate != null && next.isAfter(endDate)) {
                log.info("Recurring todo {} ended (endDate={}), leaving reminder as-is", todo.getId(), endDate);
                return;
            }
            lastOccurrence = next;
            next = advance(next, repeat);
        }

        if (next == null || (endDate != null && next.isAfter(endDate))) {
            log.info("Recurring todo {} has no future occurrence after endDate={}", todo.getId(), endDate);
            return;
        }

        if (todo.getNotifiedAt() == null || todo.getNotifiedAt().isBefore(lastOccurrence)) {
            log.info("Catch-up reminder for todo: {} - {}", todo.getId(), todo.getTitle());
            dispatchChannels(todo, buildReminderMessage(todo));
            LocalDateTime notifiedAt = LocalDateTime.now(ZoneOffset.UTC);
            todoMapper.markReminderNotified(todo.getId(), notifiedAt);
            todo.setNotifiedAt(notifiedAt);
        }

        LocalDateTime nextDue = advanceDueDate(todo.getDueDate(), todo.getReminder(), next);
        todoMapper.advanceRecurringReminder(todo.getId(), next, nextDue, LocalDateTime.now(ZoneOffset.UTC));
        log.info("Caught up recurring todo {}: next reminder at {}", todo.getId(), next);
    }

    /**
     * After notify, move reminder (and due date) to the next future occurrence.
     */
    void advanceRecurringIfNeeded(Todo todo, LocalDateTime now) {
        if (!isRecurring(todo) || todo.getReminder() == null) {
            return;
        }

        String repeat = todo.getSchedule().getRepeat();
        LocalDateTime endDate = todo.getSchedule().getEndDate();
        LocalDateTime originalReminder = todo.getReminder();
        LocalDateTime next = originalReminder;

        do {
            next = advance(next, repeat);
            if (next == null) {
                return;
            }
            if (endDate != null && next.isAfter(endDate)) {
                log.info("Recurring todo {} reached endDate={}, not scheduling further", todo.getId(), endDate);
                return;
            }
        } while (!next.isAfter(now));

        LocalDateTime nextDue = advanceDueDate(todo.getDueDate(), originalReminder, next);
        todoMapper.advanceRecurringReminder(todo.getId(), next, nextDue, LocalDateTime.now(ZoneOffset.UTC));
        todo.setReminder(next);
        todo.setDueDate(nextDue);
        log.info("Advanced recurring todo {} ({}) to next reminder {}", todo.getId(), repeat, next);
    }

    static boolean isRecurring(Todo todo) {
        if (todo.getSchedule() == null || todo.getSchedule().getRepeat() == null) {
            return false;
        }
        String repeat = todo.getSchedule().getRepeat();
        return "daily".equals(repeat) || "weekly".equals(repeat) || "monthly".equals(repeat);
    }

    static LocalDateTime advance(LocalDateTime from, String repeat) {
        if (from == null || repeat == null) {
            return null;
        }
        return switch (repeat) {
            case "daily" -> from.plusDays(1);
            case "weekly" -> from.plusWeeks(1);
            case "monthly" -> from.plusMonths(1);
            default -> null;
        };
    }

    /** Preserve gap between due date and reminder when advancing. */
    static LocalDateTime advanceDueDate(LocalDateTime dueDate, LocalDateTime oldReminder, LocalDateTime newReminder) {
        if (dueDate == null || oldReminder == null || newReminder == null) {
            return dueDate;
        }
        return dueDate.plus(Duration.between(oldReminder, newReminder));
    }

    private void dispatchChannels(Todo todo, String message) {
        String channels = todo.getNotificationChannels();
        if (channels == null || channels.isBlank()) {
            // Prefer explicit channels; empty = telegram only (avoid noisy DingTalk skips)
            channels = "telegram";
        }
        for (String channel : channels.split(",")) {
            switch (channel.trim().toLowerCase()) {
                case "telegram" -> sendToTelegram(todo, message);
                case "dingtalk" -> sendToDingTalk(todo, message);
                default -> log.warn("Unknown notification channel '{}' for todo {}", channel, todo.getId());
            }
        }
    }

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

    private String buildReminderMessage(Todo todo) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ Reminder: ").append(todo.getTitle());

        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            sb.append("\n").append(todo.getDescription(), 0, Math.min(100, todo.getDescription().length()));
        }

        if (todo.getDueDate() != null) {
            sb.append("\n📅 Due: ").append(todo.getDueDate());
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
