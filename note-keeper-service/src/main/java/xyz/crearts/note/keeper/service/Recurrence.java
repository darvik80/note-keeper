package xyz.crearts.note.keeper.service;

import xyz.crearts.note.keeper.model.Todo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;

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

    private Recurrence() {}

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static boolean isRecurring(Todo todo) {
        if (todo == null || todo.getSchedule() == null || todo.getSchedule().getRepeat() == null) {
            return false;
        }
        String repeat = todo.getSchedule().getRepeat();
        return "daily".equals(repeat) || "weekly".equals(repeat) || "monthly".equals(repeat);
    }

    public static String repeatOf(Todo todo) {
        return todo.getSchedule() != null ? todo.getSchedule().getRepeat() : null;
    }

    public static LocalDateTime advance(LocalDateTime from, String repeat) {
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
            default -> reminder;
        };
    }

    public static boolean isDoneThisPeriod(Todo todo, LocalDateTime now) {
        if (!isRecurring(todo) || todo.getLastCompletedAt() == null || todo.getReminder() == null) {
            return false;
        }
        String repeat = repeatOf(todo);
        LocalDateTime slot = currentSlot(todo.getReminder(), repeat, now);
        return isCompletedForSlot(todo.getLastCompletedAt(), slot, repeat);
    }

    static boolean isCompletedForSlot(LocalDateTime lastCompletedAt, LocalDateTime slot, String repeat) {
        if (lastCompletedAt == null || slot == null || repeat == null) {
            return false;
        }
        LocalDate last = lastCompletedAt.toLocalDate();
        LocalDate slotDate = slot.toLocalDate();
        return switch (repeat) {
            case "daily" -> last.equals(slotDate);
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
        LocalDateTime slot = currentSlot(todo.getReminder(), repeat, now);
        if (pastEndDate(todo, slot)) {
            return false;
        }

        boolean done = isCompletedForSlot(todo.getLastCompletedAt(), slot, repeat);
        LocalDateTime targetReminder = done ? advance(slot, repeat) : slot;
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
