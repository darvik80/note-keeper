package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.Test;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceTest {

    private Todo daily(LocalDateTime reminder, boolean completed, LocalDateTime lastCompletedAt) {
        Todo todo = new Todo();
        todo.setId("t1");
        todo.setReminder(reminder);
        todo.setCompleted(completed);
        todo.setLastCompletedAt(lastCompletedAt);
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("daily");
        todo.setSchedule(schedule);
        return todo;
    }

    @Test
    void currentSlot_daily_usesTodayAtReminderTime() {
        LocalDateTime reminder = LocalDateTime.of(2026, 8, 5, 15, 30);
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 1, 37);
        assertEquals(LocalDateTime.of(2026, 8, 9, 15, 30), Recurrence.currentSlot(reminder, "daily", now));
    }

    @Test
    void currentSlot_weekly_sameIsoWeekday() {
        // 2026-08-05 is Wednesday
        LocalDateTime reminder = LocalDateTime.of(2026, 8, 5, 8, 0);
        LocalDateTime friday = LocalDateTime.of(2026, 8, 7, 12, 0);
        assertEquals(LocalDateTime.of(2026, 8, 5, 8, 0), Recurrence.currentSlot(reminder, "weekly", friday));
    }

    @Test
    void eveningSpray_nextDay_uncompletesAndMovesReminder() {
        // notekeeper (1).db: «Побрызгать голову - вечер» completed 08.08, viewed 09.08
        Todo todo = daily(
                LocalDateTime.of(2026, 8, 8, 15, 30),
                true,
                LocalDateTime.of(2026, 8, 8, 3, 10, 28));
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 1, 37);

        assertTrue(Recurrence.rolloverIfNeeded(todo, now));
        assertFalse(todo.isCompleted());
        assertEquals(LocalDateTime.of(2026, 8, 9, 15, 30), todo.getReminder());
        assertNull(todo.getNotifiedAt());
    }

    @Test
    void eveningSpray_sameDay_staysDone_nextIsTomorrow() {
        Todo todo = daily(
                LocalDateTime.of(2026, 8, 8, 15, 30),
                true,
                LocalDateTime.of(2026, 8, 8, 3, 10, 28));
        LocalDateTime now = LocalDateTime.of(2026, 8, 8, 12, 0);

        assertTrue(Recurrence.rolloverIfNeeded(todo, now));
        assertTrue(todo.isCompleted());
        assertEquals(LocalDateTime.of(2026, 8, 9, 15, 30), todo.getReminder());
    }

    @Test
    void morningSpray_staleReminder_jumpsToToday() {
        Todo todo = daily(LocalDateTime.of(2026, 8, 5, 1, 30), false, null);
        todo.setNotifiedAt(LocalDateTime.of(2026, 8, 4, 1, 30, 40));
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 1, 37);

        assertTrue(Recurrence.rolloverIfNeeded(todo, now));
        assertFalse(todo.isCompleted());
        assertEquals(LocalDateTime.of(2026, 8, 9, 1, 30), todo.getReminder());
        assertNull(todo.getNotifiedAt());
    }

    @Test
    void rollover_noChange_returnsFalse() {
        Todo todo = daily(LocalDateTime.of(2026, 8, 9, 15, 30), false, null);
        assertFalse(Recurrence.rolloverIfNeeded(todo, LocalDateTime.of(2026, 8, 9, 10, 0)));
    }

    @Test
    void isDoneThisPeriod_earlyCompleteSameDay() {
        Todo todo = daily(
                LocalDateTime.of(2026, 8, 8, 15, 30),
                false,
                LocalDateTime.of(2026, 8, 8, 3, 10));
        assertTrue(Recurrence.isDoneThisPeriod(todo, LocalDateTime.of(2026, 8, 8, 12, 0)));
        assertFalse(Recurrence.isDoneThisPeriod(todo, LocalDateTime.of(2026, 8, 9, 1, 0)));
    }

    @Test
    void advance_andDueDate() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 7, 15, 30);
        assertEquals(base.plusDays(1), Recurrence.advance(base, "daily"));
        assertEquals(base.plusWeeks(1), Recurrence.advance(base, "weekly"));
        assertEquals(base.plusMonths(1), Recurrence.advance(base, "monthly"));
        assertNull(Recurrence.advance(base, "none"));

        LocalDateTime due = LocalDateTime.of(2026, 7, 7, 18, 0);
        assertEquals(LocalDateTime.of(2026, 7, 8, 18, 0),
                Recurrence.advanceDueDate(due, base, base.plusDays(1)));
    }
}
