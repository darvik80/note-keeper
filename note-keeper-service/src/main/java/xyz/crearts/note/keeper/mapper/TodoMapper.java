package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.TodoCompletionLog;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TodoMapper {

    List<Todo> findAll(@Param("completed") Boolean completed,
                       @Param("tag") String tag,
                       @Param("priority") String priority,
                       @Param("isFavorite") Boolean isFavorite,
                       @Param("isArchived") Boolean isArchived,
                       @Param("isDeleted") Boolean isDeleted,
                       @Param("ownerId") String ownerId);

    List<Todo> findSharedWithMe(@Param("userId") String userId);

    Todo findById(@Param("id") String id);

    void insert(Todo todo);

    void update(Todo todo);

    void softDelete(@Param("id") String id, @Param("deletedAt") LocalDateTime deletedAt);

    void permanentDelete(@Param("id") String id);

    void archive(@Param("id") String id);

    void restore(@Param("id") String id);

    int countByDateRange(@Param("start") String start, @Param("end") String end, @Param("ownerId") String ownerId);

    int countCompletedByDateRange(@Param("start") String start, @Param("end") String end, @Param("ownerId") String ownerId);

    List<Todo> search(@Param("query") String query,
                      @Param("tags") String tags,
                      @Param("priority") String priority,
                      @Param("ownerId") String ownerId);

    int countByPriority(@Param("priority") String priority,
                        @Param("start") String start,
                        @Param("end") String end,
                        @Param("ownerId") String ownerId);

    /**
     * Find todos with reminder time due (reminder <= now) that haven't been notified yet.
     * @param now current timestamp
     * @return list of todos with due reminders
     */
    List<Todo> findWithDueReminders(@Param("now") LocalDateTime now);

    /**
     * Mark todo reminder as notified.
     * @param id todo id
     * @param notifiedAt notification timestamp
     */
    void markReminderNotified(@Param("id") String id, @Param("notifiedAt") LocalDateTime notifiedAt);

    /**
     * Find recurring todos whose reminder is still in the past after being notified
     * (schedule was never advanced — needs catch-up).
     */
    List<Todo> findStuckRecurringReminders(@Param("now") LocalDateTime now);

    /**
     * Advance recurring todo to the next reminder occurrence.
     * Resets completed so the next cycle can be checked off again.
     */
    void advanceRecurringReminder(@Param("id") String id,
                                  @Param("reminder") LocalDateTime reminder,
                                  @Param("dueDate") LocalDateTime dueDate,
                                  @Param("updatedAt") LocalDateTime updatedAt);

    void shareWithUser(@Param("id") String id, @Param("sharedWith") String sharedWith);

    // --- Completion log methods ---

    /**
     * Insert a completion log entry for a recurring todo.
     */
    void insertCompletionLog(TodoCompletionLog log);

    /**
     * Find all completion log entries for a todo, ordered by completed_at desc.
     */
    List<TodoCompletionLog> findCompletionLog(@Param("todoId") String todoId);

    /**
     * Find completion log entries within a date range (for calendar view).
     */
    List<TodoCompletionLog> findCompletionLogByDateRange(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end,
                                                          @Param("ownerId") String ownerId);

    /**
     * Update last_completed_at on the todo table.
     */
    void updateLastCompletedAt(@Param("id") String id, @Param("lastCompletedAt") LocalDateTime lastCompletedAt);
}
