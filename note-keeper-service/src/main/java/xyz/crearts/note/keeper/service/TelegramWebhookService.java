package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.client.TelegramMarkdownUtil;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDateTime;

/**
 * Processes Telegram webhook callback queries.
 * Handles inline button actions such as marking a todo as done.
 * For recurring todos, uses TodoService.toggleComplete to properly advance occurrences.
 */
@Slf4j
@Service
public class TelegramWebhookService {

    private static final String DONE_PREFIX = "done:";

    private final TodoMapper todoMapper;
    private final TelegramClient telegramClient;
    private final UserSettingsService userSettingsService;
    private final TodoService todoService;

    public TelegramWebhookService(TodoMapper todoMapper, TelegramClient telegramClient,
                                  UserSettingsService userSettingsService, TodoService todoService) {
        this.todoMapper = todoMapper;
        this.telegramClient = telegramClient;
        this.userSettingsService = userSettingsService;
        this.todoService = todoService;
    }

    /**
     * Process a callback query from Telegram inline keyboard.
     *
     * @param webhookSecret   the webhook secret from the URL path (identifies the user)
     * @param callbackQueryId the callback query ID to answer
     * @param callbackData    the callback data from the button (e.g. "done:{todoId}")
     * @param chatId          the chat ID where the message was sent
     * @param messageId       the message ID to edit after action
     * @return true if processed successfully
     */
    public boolean processCallbackQuery(String webhookSecret, String callbackQueryId,
                                        String callbackData, String chatId, Long messageId) {
        if (callbackData == null || callbackData.isEmpty()) {
            log.warn("Empty callback data received");
            return false;
        }

        // Look up user by webhook secret
        UserSettings settings = userSettingsService.findByTelegramWebhookSecret(webhookSecret);
        if (settings == null) {
            log.warn("No user found for webhook secret");
            return false;
        }

        String botToken = settings.getTelegramBotToken();

        if (callbackData.startsWith(DONE_PREFIX)) {
            return handleDoneCallback(botToken, callbackQueryId, callbackData, chatId, messageId, settings.getId());
        }

        log.warn("Unknown callback data: {}", callbackData);
        telegramClient.answerCallbackQuery(botToken, callbackQueryId, "Unknown action");
        return false;
    }

    private boolean handleDoneCallback(String botToken, String callbackQueryId,
                                       String callbackData, String chatId, Long messageId,
                                       String userId) {
        String todoId = callbackData.substring(DONE_PREFIX.length());
        log.info("Marking todo {} as done via Telegram callback for user {}", todoId, userId);

        Todo todo = todoMapper.findById(todoId);
        if (todo == null) {
            log.warn("Todo not found: {}", todoId);
            telegramClient.answerCallbackQuery(botToken, callbackQueryId, "Todo not found");
            return false;
        }

        // Verify ownership
        if (!userId.equals(todo.getOwnerId())) {
            log.warn("User {} is not the owner of todo {}", userId, todoId);
            telegramClient.answerCallbackQuery(botToken, callbackQueryId, "Not your todo");
            return false;
        }

        if (todo.isCompleted() && !TodoService.isRecurring(todo)) {
            log.info("Todo {} is already completed", todoId);
            telegramClient.answerCallbackQuery(botToken, callbackQueryId, "Already done!");
            return true;
        }

        // Use TodoService.toggleComplete for proper recurring handling
        todoService.toggleComplete(todoId, userId);

        // Answer the callback query
        String responseText = TodoService.isRecurring(todo) ? "Done! Next occurrence scheduled." : "Done!";
        telegramClient.answerCallbackQuery(botToken, callbackQueryId, responseText);

        // Edit the message to remove the button and show completion
        if (chatId != null && messageId != null) {
            String doneText = TelegramMarkdownUtil.escapeMarkdownV2("✅ Done: " + todo.getTitle());
            telegramClient.editMessageText(botToken, chatId, messageId, doneText);
        }

        log.info("Todo {} marked as done via Telegram", todoId);
        return true;
    }
}
