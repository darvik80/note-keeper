package xyz.crearts.note.keeper.service;

import xyz.crearts.note.keeper.model.Todo;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.List;

/**
 * Recurring todo = habit series, not a one-shot that mutates forever.
 *
 * <ul>
 *   <li>{@code reminder} time-of-day (and weekday / day-of-month) is the series template</li>
 *   <li>{@code completed} means done for the <em>current period</em> only</li>
 *   <li>{@code last_completed_at} + completion log = history</li>
 *   <li>Notify does not advance reminder; {@link #rolloverIfNeeded} aligns state to {@code now}</li>
 * </ul>
 */
public final class Recurrence {

    private static final List<Integer> WEEKDAYS = List.of(1, 2, 3, 4, 5); // Mon–Fri (JS getDay)

    private Recurrence() {}

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static boolean isRecurring(Todo todo) {
        if (todo == null || todo.getSchedule() == null || todo.getSchedule().getRepeat() == null) {
            return false;
        }
        String repeat = todo.getSchedule().getRepeat();
        return "daily".equals(repeat)
                || "weekly".equals(repeat)
                || "monthly".equals(repeat)
                || "weekdays".equals(repeat)
                || "custom".equals(repeat);
    }

    public static String repeatOf(Todo todo) {
        return todo.getSchedule() != null ? todo.getSchedule().getRepeat() : null;
    }

    public static LocalDateTime advance(LocalDateTime from, String repeat) {
        return advance(from, repeat, null);
    }

    public static LocalDateTime advance(LocalDateTime from, String repeat, List<Integer> daysOfWeek) {
        if (from == null || repeat == null) {
            return null;
        }
        return switch (repeat) {
            case "daily" -> from.plusDays(1);
            case "weekly" -> from.plusWeeks(1);
            case "monthly" -> from.plusMonths(1);
            case "weekdays" -> nextMatchingDay(from, WEEKDAYS);
            case "custom" -> {
                List<Integer> days = (daysOfWeek == null || daysOfWeek.isEmpty()) ? WEEKDAYS : daysOfWeek;
                yield nextMatchingDay(from, days);
            }
            default -> null;
        };
    }

    public static LocalDateTime advanceDueDate(LocalDateTime dueDate, LocalDateTime oldReminder, LocalDateTime newReminder) {
        if (dueDate == null || oldReminder == null || newReminder == null) {
            return dueDate;
        }
        return dueDate.plus(Duration.between(oldReminder, newReminder));
    }

    /**
     * Occurrence datetime for the period containing {@code now}
     * (today / this ISO week / this month at the reminder clock time).
     */
    public static LocalDateTime currentSlot(LocalDateTime reminder, String repeat, LocalDateTime now) {
        return currentSlot(reminder, repeat, now, null);
    }

    public static LocalDateTime currentSlot(LocalDateTime reminder, String repeat, LocalDateTime now, List<Integer> daysOfWeek) {
        if (reminder == null || repeat == null || now == null) {
            return reminder;
        }
        LocalTime time = reminder.toLocalTime();
        LocalDate today = now.toLocalDate();
        return switch (repeat) {
            case "daily" -> today.atTime(time);
            case "weekly" -> today.with(reminder.getDayOfWeek()).atTime(time);
            case "monthly" -> {
                int day = Math.min(reminder.getDayOfMonth(), today.lengthOfMonth());
                yield today.withDayOfMonth(day).atTime(time);
            }
            case "weekdays" -> currentMatchingDaySlot(today, time, WEEKDAYS);
            case "custom" -> {
                List<Integer> days = (daysOfWeek == null || daysOfWeek.isEmpty()) ? WEEKDAYS : daysOfWeek;
                yield currentMatchingDaySlot(today, time, days);
            }
            default -> reminder;
        };
    }

    /** Prefer today if it matches; otherwise last matching day on or before today. */
    private static LocalDateTime currentMatchingDaySlot(LocalDate today, LocalTime time, List<Integer> days) {
        LocalDate d = today;
        for (int i = 0; i < 8; i++) {
            if (days.contains(toJsDayOfWeek(d.getDayOfWeek()))) {
                return d.atTime(time);
            }
            d = d.minusDays(1);
        }
        return today.atTime(time);
    }

    private static LocalDateTime nextMatchingDay(LocalDateTime from, List<Integer> days) {
        LocalDateTime candidate = from.plusDays(1);
        for (int i = 0; i < 8; i++) {
            if (days.contains(toJsDayOfWeek(candidate.getDayOfWeek()))) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        return null;
    }

    /** Java Mon=1..Sun=7 → JS Sun=0..Sat=6 */
    static int toJsDayOfWeek(DayOfWeek day) {
        return day.getValue() % 7;
    }

    public static boolean isDoneThisPeriod(Todo todo, LocalDateTime now) {
        if (!isRecurring(todo) || todo.getLastCompletedAt() == null || todo.getReminder() == null) {
            return false;
        }
        String repeat = repeatOf(todo);
        LocalDateTime slot = currentSlot(todo.getReminder(), repeat, now, daysOf(todo));
        return isCompletedForSlot(todo.getLastCompletedAt(), slot, repeat);
    }

    static boolean isCompletedForSlot(LocalDateTime lastCompletedAt, LocalDateTime slot, String repeat) {
        if (lastCompletedAt == null || slot == null || repeat == null) {
            return false;
        }
        LocalDate last = lastCompletedAt.toLocalDate();
        LocalDate slotDate = slot.toLocalDate();
        return switch (repeat) {
            case "daily", "weekdays", "custom" -> last.equals(slotDate);
            case "weekly" -> {
                WeekFields wf = WeekFields.ISO;
                yield last.get(wf.weekBasedYear()) == slotDate.get(wf.weekBasedYear())
                        && last.get(wf.weekOfWeekBasedYear()) == slotDate.get(wf.weekOfWeekBasedYear());
            }
            case "monthly" -> last.getYear() == slotDate.getYear() && last.getMonth() == slotDate.getMonth();
            default -> false;
        };
    }

    public static boolean pastEndDate(Todo todo, LocalDateTime slot) {
        if (todo == null || todo.getSchedule() == null || todo.getSchedule().getEndDate() == null || slot == null) {
            return false;
        }
        return slot.isAfter(todo.getSchedule().getEndDate());
    }

    private static List<Integer> daysOf(Todo todo) {
        if (todo.getSchedule() == null) {
            return null;
        }
        return todo.getSchedule().getDaysOfWeek();
    }

    /**
     * Align {@code completed} / {@code reminder} / {@code notifiedAt} with the current period.
     *
     * @return true if todo was mutated and should be persisted
     */
    public static boolean rolloverIfNeeded(Todo todo, LocalDateTime now) {
        if (!isRecurring(todo) || todo.getReminder() == null || now == null) {
            return false;
        }
        String repeat = repeatOf(todo);
        List<Integer> days = daysOf(todo);
        LocalDateTime slot = currentSlot(todo.getReminder(), repeat, now, days);
        if (pastEndDate(todo, slot)) {
            return false;
        }

        boolean done = isCompletedForSlot(todo.getLastCompletedAt(), slot, repeat);
        LocalDateTime targetReminder = done ? advance(slot, repeat, days) : slot;
        if (done && targetReminder != null && pastEndDate(todo, targetReminder)) {
            targetReminder = slot;
        }
        if (targetReminder == null) {
            return false;
        }

        boolean reminderChanged = !targetReminder.equals(todo.getReminder());
        boolean completedChanged = todo.isCompleted() != done;
        if (!reminderChanged && !completedChanged) {
            return false;
        }

        LocalDateTime oldReminder = todo.getReminder();
        todo.setReminder(targetReminder);
        todo.setDueDate(advanceDueDate(todo.getDueDate(), oldReminder, targetReminder));
        todo.setCompleted(done);
        if (reminderChanged) {
            todo.setNotifiedAt(null);
        }
        todo.setUpdatedAt(now);
        return true;
    }
}
