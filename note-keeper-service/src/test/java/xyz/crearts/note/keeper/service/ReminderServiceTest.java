package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void computeNextReminder_daily_advancesOneDayFromCurrentReminder() {
        Todo todo = new Todo();
        todo.setReminder(LocalDateTime.of(2026, 3, 10, 9, 0));
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("daily");
        todo.setSchedule(schedule);

        LocalDateTime next = reminderService.computeNextReminder(todo);
        assertEquals(LocalDateTime.of(2026, 3, 11, 9, 0), next);
    }

    @Test
    void computeNextReminder_weekdays_skipsWeekend() {
        Todo todo = new Todo();
        // Friday
        todo.setReminder(LocalDateTime.of(2026, 3, 13, 9, 0));
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("weekdays");
        todo.setSchedule(schedule);

        LocalDateTime next = reminderService.computeNextReminder(todo);
        // Next Monday
        assertEquals(LocalDateTime.of(2026, 3, 16, 9, 0), next);
    }

    @Test
    void computeNextReminder_custom_usesDaysOfWeek() {
        Todo todo = new Todo();
        // Wednesday
        todo.setReminder(LocalDateTime.of(2026, 3, 11, 8, 30));
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("custom");
        schedule.setDaysOfWeek(List.of(1, 3)); // Mon, Wed
        todo.setSchedule(schedule);

        LocalDateTime next = reminderService.computeNextReminder(todo);
        // Next Monday
        assertEquals(LocalDateTime.of(2026, 3, 16, 8, 30), next);
    }

    @Test
    void computeNextReminder_none_returnsNull() {
        Todo todo = new Todo();
        todo.setReminder(LocalDateTime.of(2026, 3, 10, 9, 0));
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("none");
        todo.setSchedule(schedule);

        assertNull(reminderService.computeNextReminder(todo));
    }

    @Test
    void computeNextReminder_respectsEndDate() {
        Todo todo = new Todo();
        todo.setReminder(LocalDateTime.of(2026, 3, 10, 9, 0));
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("daily");
        schedule.setEndDate(LocalDateTime.of(2026, 3, 10, 23, 59));
        todo.setSchedule(schedule);

        assertNull(reminderService.computeNextReminder(todo));
    }
}
