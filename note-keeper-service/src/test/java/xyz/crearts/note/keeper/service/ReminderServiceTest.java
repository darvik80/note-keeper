package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.mapper.TodoMapper;
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

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(todoMapper, telegramClient, dingTalkClient, userSettingsService);
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
        when(userSettingsService.getDecryptedSettings("user-1")).thenReturn(settings);
        when(telegramClient.sendMessage(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void advance_daily_weekly_monthly() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 7, 15, 30);
        assertEquals(base.plusDays(1), ReminderService.advance(base, "daily"));
        assertEquals(base.plusWeeks(1), ReminderService.advance(base, "weekly"));
        assertEquals(base.plusMonths(1), ReminderService.advance(base, "monthly"));
        assertNull(ReminderService.advance(base, "none"));
    }

    @Test
    void advanceRecurringIfNeeded_daily_movesReminderPastNow() {
        LocalDateTime reminder = LocalDateTime.of(2026, 7, 7, 15, 30);
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 15, 31);
        Todo todo = dailyTodo("t1", reminder, now);

        reminderService.advanceRecurringIfNeeded(todo, now);

        ArgumentCaptor<LocalDateTime> nextCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(todoMapper).advanceRecurringReminder(eq("t1"), nextCap.capture(), isNull(), any());
        assertEquals(LocalDateTime.of(2026, 7, 8, 15, 30), nextCap.getValue());
        assertEquals(LocalDateTime.of(2026, 7, 8, 15, 30), todo.getReminder());
    }

    @Test
    void advanceRecurringIfNeeded_skipsNoneRepeat() {
        Todo todo = dailyTodo("t1", LocalDateTime.of(2026, 7, 7, 15, 30), null);
        todo.getSchedule().setRepeat("none");

        reminderService.advanceRecurringIfNeeded(todo, LocalDateTime.of(2026, 7, 7, 16, 0));

        verify(todoMapper, never()).advanceRecurringReminder(any(), any(), any(), any());
    }

    @Test
    void advanceRecurringIfNeeded_respectsEndDate() {
        LocalDateTime reminder = LocalDateTime.of(2026, 7, 7, 15, 30);
        Todo todo = dailyTodo("t1", reminder, null);
        todo.getSchedule().setEndDate(LocalDateTime.of(2026, 7, 7, 23, 59));

        reminderService.advanceRecurringIfNeeded(todo, LocalDateTime.of(2026, 7, 7, 16, 0));

        verify(todoMapper, never()).advanceRecurringReminder(any(), any(), any(), any());
    }

    @Test
    void checkReminders_sendsAndAdvancesDaily() {
        LocalDateTime reminder = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);
        Todo todo = dailyTodo("t1", reminder, null);
        stubTelegramOk();

        when(todoMapper.findWithDueReminders(any())).thenReturn(List.of(todo));
        when(todoMapper.findStuckRecurringReminders(any())).thenReturn(Collections.emptyList());

        reminderService.checkReminders();

        verify(telegramClient).sendMessage(eq("token"), eq("chat"), contains("Daily task"));
        verify(todoMapper).markReminderNotified(eq("t1"), any());
        verify(todoMapper).advanceRecurringReminder(eq("t1"), any(), isNull(), any());
    }

    @Test
    void checkReminders_catchUpStuckDaily_notifiesOnceAndJumpsAhead() {
        // Real bug case from notekeeper.db: daily todo notified once in early July, never advanced
        LocalDateTime reminder = LocalDateTime.of(2026, 7, 7, 15, 30);
        LocalDateTime notifiedAt = LocalDateTime.of(2026, 7, 7, 15, 30, 6);
        Todo todo = dailyTodo("stuck-1", reminder, notifiedAt);
        stubTelegramOk();

        when(todoMapper.findWithDueReminders(any())).thenReturn(Collections.emptyList());
        when(todoMapper.findStuckRecurringReminders(any())).thenReturn(List.of(todo));

        reminderService.checkReminders();

        verify(telegramClient, times(1)).sendMessage(eq("token"), eq("chat"), contains("Daily task"));
        verify(todoMapper).markReminderNotified(eq("stuck-1"), any());

        ArgumentCaptor<LocalDateTime> nextCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(todoMapper).advanceRecurringReminder(eq("stuck-1"), nextCap.capture(), isNull(), any());
        assertTrue(nextCap.getValue().isAfter(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
    }

    @Test
    void advanceDueDate_preservesOffsetFromReminder() {
        LocalDateTime oldReminder = LocalDateTime.of(2026, 7, 7, 9, 0);
        LocalDateTime due = LocalDateTime.of(2026, 7, 7, 18, 0);
        LocalDateTime newReminder = LocalDateTime.of(2026, 7, 8, 9, 0);

        LocalDateTime nextDue = ReminderService.advanceDueDate(due, oldReminder, newReminder);
        assertEquals(LocalDateTime.of(2026, 7, 8, 18, 0), nextDue);
    }

    @Test
    void advanceDueDate_preservesNanos() {
        LocalDateTime oldReminder = LocalDateTime.of(2026, 7, 7, 9, 0, 0, 500_000_000);
        LocalDateTime due = LocalDateTime.of(2026, 7, 7, 9, 0, 0, 750_000_000);
        LocalDateTime newReminder = LocalDateTime.of(2026, 7, 8, 9, 0, 0, 500_000_000);

        assertEquals(
                LocalDateTime.of(2026, 7, 8, 9, 0, 0, 750_000_000),
                ReminderService.advanceDueDate(due, oldReminder, newReminder));
    }

    @Test
    void dispatchDefaultChannel_isTelegramOnly() {
        LocalDateTime reminder = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        Todo todo = dailyTodo("t1", reminder, null);
        todo.setNotificationChannels(null);
        stubTelegramOk();

        when(todoMapper.findWithDueReminders(any())).thenReturn(List.of(todo));
        when(todoMapper.findStuckRecurringReminders(any())).thenReturn(Collections.emptyList());

        reminderService.checkReminders();

        verify(telegramClient, times(1)).sendMessage(anyString(), anyString(), anyString());
        verify(dingTalkClient, never()).sendMessage(anyString(), anyString(), anyString());
    }
}
