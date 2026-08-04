package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.client.TelegramMarkdownUtil;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookServiceTest {

    @Mock private TodoMapper todoMapper;
    @Mock private TelegramClient telegramClient;
    @Mock private UserSettingsService userSettingsService;

    private TelegramWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new TelegramWebhookService(todoMapper, telegramClient, userSettingsService);
    }

    private UserSettings mockSettings(String userId) {
        UserSettings settings = new UserSettings();
        settings.setId(userId);
        settings.setTelegramBotToken("bot-token");
        settings.setTelegramChatId("chat-id");
        settings.setTelegramWebhookSecret("my-secret");
        return settings;
    }

    private Todo mockTodo(String id, String ownerId, boolean completed) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle("Test todo");
        todo.setOwnerId(ownerId);
        todo.setCompleted(completed);
        return todo;
    }

    @Test
    void processCallbackQuery_doneAction_marksTodoAsCompleted() {
        UserSettings settings = mockSettings("user-1");
        Todo todo = mockTodo("todo-123", "user-1", false);

        when(userSettingsService.findByTelegramWebhookSecret("my-secret")).thenReturn(settings);
        when(todoMapper.findById("todo-123")).thenReturn(todo);
        when(telegramClient.answerCallbackQuery(anyString(), anyString(), anyString())).thenReturn(true);
        when(telegramClient.editMessageText(anyString(), anyString(), anyLong(), anyString())).thenReturn(true);

        boolean result = webhookService.processCallbackQuery(
                "my-secret", "callback-1", "done:todo-123", "12345", 42L);

        assertTrue(result);
        assertTrue(todo.isCompleted());
        verify(todoMapper).update(todo);
        verify(telegramClient).answerCallbackQuery("bot-token", "callback-1", "Done!");
        verify(telegramClient).editMessageText(eq("bot-token"), eq("12345"), eq(42L), contains("Done"));
    }

    @Test
    void processCallbackQuery_unknownSecret_returnsFalse() {
        when(userSettingsService.findByTelegramWebhookSecret("bad-secret")).thenReturn(null);

        boolean result = webhookService.processCallbackQuery(
                "bad-secret", "cb-1", "done:todo-1", "123", 1L);

        assertFalse(result);
        verify(todoMapper, never()).findById(any());
    }

    @Test
    void processCallbackQuery_todoNotFound_returnsFalse() {
        UserSettings settings = mockSettings("user-1");
        when(userSettingsService.findByTelegramWebhookSecret("my-secret")).thenReturn(settings);
        when(todoMapper.findById("nonexistent")).thenReturn(null);
        when(telegramClient.answerCallbackQuery(anyString(), anyString(), anyString())).thenReturn(true);

        boolean result = webhookService.processCallbackQuery(
                "my-secret", "cb-1", "done:nonexistent", "123", 1L);

        assertFalse(result);
        verify(telegramClient).answerCallbackQuery("bot-token", "cb-1", "Todo not found");
    }

    @Test
    void processCallbackQuery_notOwner_returnsFalse() {
        UserSettings settings = mockSettings("user-1");
        Todo todo = mockTodo("todo-1", "other-user", false);

        when(userSettingsService.findByTelegramWebhookSecret("my-secret")).thenReturn(settings);
        when(todoMapper.findById("todo-1")).thenReturn(todo);
        when(telegramClient.answerCallbackQuery(anyString(), anyString(), anyString())).thenReturn(true);

        boolean result = webhookService.processCallbackQuery(
                "my-secret", "cb-1", "done:todo-1", "123", 1L);

        assertFalse(result);
        verify(todoMapper, never()).update(any());
        verify(telegramClient).answerCallbackQuery("bot-token", "cb-1", "Not your todo");
    }

    @Test
    void processCallbackQuery_alreadyCompleted_returnsTrue() {
        UserSettings settings = mockSettings("user-1");
        Todo todo = mockTodo("todo-1", "user-1", true);

        when(userSettingsService.findByTelegramWebhookSecret("my-secret")).thenReturn(settings);
        when(todoMapper.findById("todo-1")).thenReturn(todo);
        when(telegramClient.answerCallbackQuery(anyString(), anyString(), anyString())).thenReturn(true);

        boolean result = webhookService.processCallbackQuery(
                "my-secret", "cb-1", "done:todo-1", "123", 1L);

        assertTrue(result);
        verify(todoMapper, never()).update(any());
        verify(telegramClient).answerCallbackQuery("bot-token", "cb-1", "Already done!");
    }

    @Test
    void processCallbackQuery_emptyData_returnsFalse() {
        boolean result = webhookService.processCallbackQuery(
                "my-secret", "cb-1", "", "123", 1L);

        assertFalse(result);
    }

    @Test
    void escapeMarkdownV2_escapesCorrectly() {
        assertEquals("hello\\.", TelegramMarkdownUtil.escapeMarkdownV2("hello."));
        assertEquals("\\*bold\\*", TelegramMarkdownUtil.escapeMarkdownV2("*bold*"));
        assertEquals("", TelegramMarkdownUtil.escapeMarkdownV2(null));
    }
}
