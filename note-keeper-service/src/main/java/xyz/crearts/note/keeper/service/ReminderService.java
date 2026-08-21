package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Service for handling Todo reminders and sending notifications.
 * Checks for due reminders every minute and sends notifications via Telegram/DingTalk.
 */
@Slf4j
@Service
public class ReminderService {

    private static final List<Integer> WEEKDAYS = List.of(1, 2, 3, 4, 5); // Mon–Fri (JS getDay)

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

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
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

        String channels = todo.getNotificationChannels();
        if (channels == null || channels.isBlank()) {
            log.warn("No notification channels set for todo {}; skipping send", todo.getId());
        } else {
            String[] channelArray = channels.split(",");
            for (String channel : channelArray) {
                String trimmedChannel = channel.trim();
                if ("telegram".equalsIgnoreCase(trimmedChannel)) {
                    sendToTelegram(todo, message);
                } else if ("dingtalk".equalsIgnoreCase(trimmedChannel)) {
                    sendToDingTalk(todo, message);
                }
            }
        }

        LocalDateTime notifiedAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime next = computeNextReminder(todo);
        if (next != null) {
            todoMapper.advanceReminder(todo.getId(), next, notifiedAt);
            log.info("Advanced recurring reminder for todo {} to {}", todo.getId(), next);
        } else {
            todoMapper.markReminderNotified(todo.getId(), notifiedAt);
        }
    }

    /**
     * Next reminder from the series anchored at the current reminder datetime
     * (which itself was seeded from due/start when the todo was created).
     */
    LocalDateTime computeNextReminder(Todo todo) {
        LocalDateTime current = todo.getReminder();
        if (current == null) {
            return null;
        }
        Todo.Schedule schedule = todo.getSchedule();
        if (schedule == null || schedule.getRepeat() == null || "none".equalsIgnoreCase(schedule.getRepeat())) {
            return null;
        }

        LocalDateTime next = switch (schedule.getRepeat().toLowerCase()) {
            case "daily" -> current.plusDays(1);
            case "weekly" -> current.plusWeeks(1);
            case "monthly" -> current.plusMonths(1);
            case "weekdays" -> nextMatchingDay(current, WEEKDAYS);
            case "custom" -> {
                List<Integer> days = schedule.getDaysOfWeek();
                yield (days == null || days.isEmpty()) ? null : nextMatchingDay(current, days);
            }
            default -> null;
        };

        if (next == null) {
            return null;
        }
        LocalDateTime end = schedule.getEndDate();
        if (end != null && next.toLocalDate().isAfter(end.toLocalDate())) {
            return null;
        }
        return next;
    }

    /**
     * Next datetime after {@code from} whose day-of-week is in {@code days}
     * (0=Sun … 6=Sat, matching JS {@code Date.getDay()}).
     */
    private static LocalDateTime nextMatchingDay(LocalDateTime from, List<Integer> days) {
        LocalDateTime candidate = from.plusDays(1);
        for (int i = 0; i < 8; i++) {
            int jsDow = toJsDayOfWeek(candidate.getDayOfWeek());
            if (days.contains(jsDow)) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        return null;
    }

    private static int toJsDayOfWeek(DayOfWeek day) {
        // Java Mon=1..Sun=7 → JS Sun=0..Sat=6
        return day.getValue() % 7;
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
