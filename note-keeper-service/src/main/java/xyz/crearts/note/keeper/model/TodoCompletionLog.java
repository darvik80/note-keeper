package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Records each completion event for a recurring todo.
 * Used by the calendar view to show which days a recurring task was completed.
 */
@Data
public class TodoCompletionLog {
    private String id;
    private String todoId;
    private LocalDateTime completedAt;
    private LocalDateTime occurrenceReminder;
    private LocalDateTime occurrenceDueDate;
}
