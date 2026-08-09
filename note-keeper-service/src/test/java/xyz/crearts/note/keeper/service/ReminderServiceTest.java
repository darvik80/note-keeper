package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.client.TelegramMarkdownUtil;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.mapper.UserSettingsMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.UserSettings;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock private TodoMapper todoMapper;
    @Mock private TelegramClient telegramClient;
    @Mock private DingTalkClient dingTalkClient;
    @Mock private UserSettingsService userSettingsService;
    @Mock private UserSettingsMapper userSettingsMapper;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(todoMapper, telegramClient, dingTalkClient, userSettingsService, userSettingsMapper);
    }

    private Todo dailyTodo(String id, LocalDateTime reminder, LocalDateTime notifiedAt) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle("Daily task");
        todo.setOwnerId("user-1");
        todo.setReminder(reminder);
        todo.setNotifiedAt(notifiedAt);
        todo.setNotificationChannels("telegram");
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("daily");
        todo.setSchedule(schedule);
        return todo;
    }

    private void stubTelegramOk() {
        UserSettings settings = new UserSettings();
        settings.setTelegramBotToken("token");
        settings.setTelegramChatId("chat");
        settings.setTelegramWebhookSecret("webhook-secret");
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(settings);
        when(telegramClient.sendMessage(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(true);
    }

    @Test
    void checkReminders_sendsButDoesNotAdvanceDaily() {
        LocalDateTime reminder = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);
        Todo todo = dailyTodo("t1", reminder, null);
        stubTelegramOk();

        when(todoMapper.findRecurringActive(null)).thenReturn(Collections.emptyList());
        when(todoMapper.findWithDueReminders(any())).thenReturn(List.of(todo));

        reminderService.checkReminders();

        verify(telegramClient).sendMessage(eq("token"), eq("chat"), contains("Daily task"), eq("MarkdownV2"), any());
        verify(todoMapper).markReminderNotified(eq("t1"), any());
        verify(todoMapper, never()).update(any());
    }

    @Test
    void checkReminders_rolloverPersistsBeforeNotify() {
        Todo stuck = dailyTodo("stuck-1", LocalDateTime.of(2026, 8, 8, 15, 30), null);
        stuck.setCompleted(true);
        stuck.setLastCompletedAt(LocalDateTime.of(2026, 8, 8, 3, 10));
        when(todoMapper.findRecurringActive(null)).thenReturn(List.of(stuck));
        when(todoMapper.findWithDueReminders(any())).thenReturn(Collections.emptyList());

        reminderService.checkReminders();

        verify(todoMapper).update(argThat(t ->
                "stuck-1".equals(t.getId()) && !t.isCompleted()));
        verify(telegramClient, never()).sendMessage(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void dispatchDefaultChannel_isTelegramOnly() {
        LocalDateTime reminder = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        Todo todo = dailyTodo("t1", reminder, null);
        todo.setNotificationChannels(null);
        stubTelegramOk();

        when(todoMapper.findRecurringActive(null)).thenReturn(Collections.emptyList());
        when(todoMapper.findWithDueReminders(any())).thenReturn(List.of(todo));

        reminderService.checkReminders();

        verify(telegramClient, times(1)).sendMessage(anyString(), anyString(), anyString(), anyString(), any());
        verify(dingTalkClient, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void escapeMarkdownV2_escapesSpecialChars() {
        assertEquals("hello\\.", TelegramMarkdownUtil.escapeMarkdownV2("hello."));
        assertEquals("\\*bold\\*", TelegramMarkdownUtil.escapeMarkdownV2("*bold*"));
        assertEquals("no escape here", TelegramMarkdownUtil.escapeMarkdownV2("no escape here"));
        assertEquals("\\(paren\\)", TelegramMarkdownUtil.escapeMarkdownV2("(paren)"));
        assertEquals("a\\-b", TelegramMarkdownUtil.escapeMarkdownV2("a-b"));
        assertEquals("", TelegramMarkdownUtil.escapeMarkdownV2(null));
    }

    @Test
    void bold_wrapsEscapedTextInAsterisks() {
        assertEquals("*hello*", TelegramMarkdownUtil.bold("hello"));
        assertEquals("*a\\.b*", TelegramMarkdownUtil.bold("a.b"));
        assertEquals("*\\*x\\**", TelegramMarkdownUtil.bold("*x*"));
        assertEquals("**", TelegramMarkdownUtil.bold(null));
    }

    @Test
    void field_buildsEmojiLabelValueLine() {
        assertEquals("📅 *Due:* 04 Aug", TelegramMarkdownUtil.field("📅", "Due:", "04 Aug"));
        assertEquals("🔁 *Repeat:* daily", TelegramMarkdownUtil.field("🔁", "Repeat:", "daily"));
        assertEquals(" *Priority:* high", TelegramMarkdownUtil.field("", "Priority:", "high"));
        // value with special chars is escaped
        assertEquals("📅 *Due:* a\\.b", TelegramMarkdownUtil.field("📅", "Due:", "a.b"));
    }

    @Test
    void buildReminderMessageMarkdownV2_containsBoldAndEscaping() {
        Todo todo = new Todo();
        todo.setId("t1");
        todo.setTitle("Test task");
        todo.setPriority("high");
        todo.setDueDate(LocalDateTime.of(2026, 8, 4, 15, 0));
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("daily");
        todo.setSchedule(schedule);

        String msg = reminderService.buildReminderMessageMarkdownV2(todo);

        assertTrue(msg.contains("*Reminder*"));
        assertTrue(msg.contains("*Test task*"));
        assertTrue(msg.contains("🔴"));
        assertTrue(msg.contains("*Priority:*"));
        assertTrue(msg.contains("*Due:*"));
        assertTrue(msg.contains("*Repeat:*"));
    }
}
