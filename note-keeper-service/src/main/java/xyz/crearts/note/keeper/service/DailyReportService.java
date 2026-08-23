package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.client.TelegramMarkdownUtil;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.mapper.UserSettingsMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Daily report scheduler.
 * Sends a summary of uncompleted todos at a user-configured time via Telegram/DingTalk.
 * Templates are user-customizable with variable substitution.
 */
@Slf4j
@Service
public class DailyReportService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_KEY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String DEFAULT_BODY_TEMPLATE =
            "Daily Report — {date}\n\nYou have {todo_count} pending todo(s):\n\n{todo_list}";
    private static final String DEFAULT_ITEM_TEMPLATE =
            "{priority_icon} {title}{due_date}{tags}";

    private final TodoMapper todoMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final UserSettingsService userSettingsService;
    private final TelegramClient telegramClient;
    private final DingTalkClient dingTalkClient;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    public DailyReportService(TodoMapper todoMapper, UserSettingsMapper userSettingsMapper,
                              UserSettingsService userSettingsService,
                              TelegramClient telegramClient, DingTalkClient dingTalkClient) {
        this.todoMapper = todoMapper;
        this.userSettingsMapper = userSettingsMapper;
        this.userSettingsService = userSettingsService;
        this.telegramClient = telegramClient;
        this.dingTalkClient = dingTalkClient;
    }

    /**
     * Every 60 seconds: check if any user's daily report is due.
     */
    @Scheduled(fixedRate = 60000)
    public void checkDailyReports() {
        List<UserSettings> enabledUsers;
        try {
            enabledUsers = userSettingsMapper.findDailyReportEnabled();
        } catch (Exception e) {
            log.debug("Daily report query failed (columns may not exist yet): {}", e.getMessage());
            return;
        }

        if (enabledUsers.isEmpty()) return;

        // Use UTC (same pattern as ReminderService) — frontend stores time as UTC
        LocalDateTime now = Recurrence.nowUtc();
        String currentTime = now.toLocalTime().format(TIME_FMT);
        String today = now.toLocalDate().format(DATE_KEY_FMT);

        for (UserSettings raw : enabledUsers) {
            try {
                UserSettings settings = userSettingsService.getDecryptedSettings(raw.getId());
                if (settings == null || !settings.isDailyReportEnabled()) continue;

                String reportTime = settings.getDailyReportTime();
                if (reportTime == null || !reportTime.equals(currentTime)) continue;

                // Already sent today?
                if (today.equals(settings.getDailyReportLastSent())) continue;

                sendDailyReport(settings, today);

                // Mark as sent
                userSettingsMapper.updateDailyReportLastSent(settings.getId(), today);
                log.info("Daily report sent for user: {}", settings.getId());
            } catch (Exception e) {
                log.error("Failed to send daily report for user {}: {}", raw.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send a test daily report immediately (for preview/test from UI).
     */
    public String generateReport(String userId) {
        UserSettings settings = userSettingsService.getDecryptedSettings(userId);
        if (settings == null) {
            return "No settings found for user";
        }
        return buildReportText(settings, Recurrence.nowUtc().toLocalDate().format(DATE_KEY_FMT));
    }

    /**
     * Send a test daily report to the user's configured channels.
     */
    public void sendTestReport(String userId) {
        UserSettings settings = userSettingsService.getDecryptedSettings(userId);
        if (settings == null) {
            throw new IllegalArgumentException("No settings found for user");
        }
        String reportText = buildReportText(settings, Recurrence.nowUtc().toLocalDate().format(DATE_KEY_FMT));
        dispatchChannels(settings, reportText);
    }

    private void sendDailyReport(UserSettings settings, String today) {
        String reportText = buildReportText(settings, today);
        dispatchChannels(settings, reportText);
    }

    String buildReportText(UserSettings settings, String today) {
        List<Todo> todos = todoMapper.findAll(false, null, null, null, false, false, settings.getId());

        String bodyTemplate = settings.getDailyReportTemplate();
        if (bodyTemplate == null || bodyTemplate.isBlank()) {
            bodyTemplate = DEFAULT_BODY_TEMPLATE;
        }
        String itemTemplate = settings.getDailyReportItemTemplate();
        if (itemTemplate == null || itemTemplate.isBlank()) {
            itemTemplate = DEFAULT_ITEM_TEMPLATE;
        }

        // Build todo list
        StringBuilder todoList = new StringBuilder();
        for (Todo todo : todos) {
            String item = renderItemTemplate(itemTemplate, todo);
            todoList.append(item).append("\n");
        }

        // Render body
        String date = formatDate(today);
        return bodyTemplate
                .replace("{date}", date != null ? date : today)
                .replace("{todo_count}", String.valueOf(todos.size()))
                .replace("{todo_list}", todoList.toString().stripTrailing());
    }

    private String renderItemTemplate(String template, Todo todo) {
        String result = template;
        result = result.replace("{title}", todo.getTitle() != null ? todo.getTitle() : "");
        result = result.replace("{priority}", todo.getPriority() != null ? todo.getPriority() : "medium");
        result = result.replace("{priority_icon}", priorityIcon(todo.getPriority()));

        if (todo.getDueDate() != null) {
            result = result.replace("{due_date}", todo.getDueDate().format(DATE_FMT));
        } else {
            result = result.replace("{due_date}", "");
        }

        if (todo.getTags() != null && !todo.getTags().isEmpty()) {
            String tags = "#" + String.join(" #", todo.getTags());
            result = result.replace("{tags}", tags);
        } else {
            result = result.replace("{tags}", "");
        }

        String link = buildTodoLink(todo.getId());
        result = result.replace("{link}", link);

        // Clean up extra whitespace from empty replacements
        return result.replaceAll("\\s{2,}", " ").strip();
    }

    private String priorityIcon(String priority) {
        if (priority == null) return "🟡";
        return switch (priority) {
            case "high" -> "🔴";
            case "low" -> "🟢";
            default -> "🟡";
        };
    }

    private String buildTodoLink(String todoId) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + "/#/todos/" + todoId;
        }
        return "#/todos/" + todoId;
    }

    private String formatDate(String dateKey) {
        try {
            return LocalDate.parse(dateKey).format(DATE_FMT);
        } catch (Exception e) {
            return dateKey;
        }
    }

    private void dispatchChannels(UserSettings settings, String reportText) {
        String channels = settings.getDailyReportChannels();
        if (channels == null || channels.isBlank()) {
            channels = "telegram";
        }
        for (String channel : channels.split(",")) {
            switch (channel.trim().toLowerCase()) {
                case "telegram" -> sendToTelegram(settings, reportText);
                case "dingtalk" -> sendToDingTalk(settings, reportText);
                default -> log.warn("Unknown daily report channel: {}", channel);
            }
        }
    }

    private void sendToTelegram(UserSettings settings, String text) {
        if (settings.getTelegramBotToken() == null || settings.getTelegramChatId() == null) {
            log.warn("Telegram credentials not set for user: {}", settings.getId());
            return;
        }
        // Escape for MarkdownV2
        String mdText = TelegramMarkdownUtil.escapeMarkdownV2(text);
        boolean ok = telegramClient.sendMessage(
                settings.getTelegramBotToken(),
                settings.getTelegramChatId(),
                mdText,
                "MarkdownV2",
                null
        );
        if (!ok) {
            // Fallback to plain text
            telegramClient.sendMessage(
                    settings.getTelegramBotToken(),
                    settings.getTelegramChatId(),
                    text
            );
        }
    }

    private void sendToDingTalk(UserSettings settings, String text) {
        if (settings.getDingtalkWebhook() == null) {
            log.warn("DingTalk webhook not set for user: {}", settings.getId());
            return;
        }
        dingTalkClient.sendMessage(settings.getDingtalkWebhook(), settings.getDingtalkSecret(), text);
    }
}
