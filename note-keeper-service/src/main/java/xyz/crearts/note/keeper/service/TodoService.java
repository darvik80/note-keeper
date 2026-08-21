package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.crearts.note.keeper.dto.TodoInput;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.model.TodoCompletionLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final AttachmentMapper attachmentMapper;
    private final NotificationService notificationService;
    private final TagSyncService tagSyncService;
    private final ResourceAccessService resourceAccess;

    public TodoService(TodoMapper todoMapper, AttachmentMapper attachmentMapper,
                       NotificationService notificationService, TagSyncService tagSyncService,
                       ResourceAccessService resourceAccess) {
        this.todoMapper = todoMapper;
        this.attachmentMapper = attachmentMapper;
        this.notificationService = notificationService;
        this.tagSyncService = tagSyncService;
        this.resourceAccess = resourceAccess;
    }

    public List<Todo> findAll(Boolean completed, String tag, String priority,
                              Boolean isFavorite, Boolean isArchived, Boolean isDeleted, String ownerId) {
        rolloverOwnerRecurring(ownerId);
        return todoMapper.findAll(completed, tag, priority, isFavorite, isArchived, isDeleted, ownerId);
    }

    public List<Todo> findSharedWithMe(String userId) {
        List<Todo> todos = todoMapper.findSharedWithMe(userId);
        LocalDateTime now = Recurrence.nowUtc();
        for (Todo todo : todos) {
            persistRollover(todo, now);
        }
        return todos;
    }

    public Todo findById(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoRead(todo, userId);
        persistRollover(todo, Recurrence.nowUtc());
        return todo;
    }

    private Todo loadTodo(String id) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + id);
        }
        return todo;
    }

    @Transactional
    public Todo create(TodoInput input, String ownerId) {
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID().toString());
        todo.setTitle(input.getTitle());
        todo.setDescription(input.getDescription());
        todo.setCompleted(input.getCompleted() != null && input.getCompleted());
        todo.setTags(input.getTags() != null ? input.getTags() : new ArrayList<>());
        todo.setPriority(input.getPriority() != null ? input.getPriority() : "medium");
        todo.setFavorite(input.getIsFavorite() != null && input.getIsFavorite());
        todo.setArchived(false);
        todo.setDeleted(false);
        todo.setOwnerId(ownerId);
        todo.setSharedWith("[]");
        todo.setDueDate(parseDate(input.getDueDate()));
        todo.setReminder(parseDate(input.getReminder()));
        todo.setNotificationChannels(input.getNotificationChannels());

        if (input.getSchedule() != null) {
            todo.setSchedule(convertToSchedule(input.getSchedule()));
        } else {
            Todo.Schedule schedule = new Todo.Schedule();
            schedule.setRepeat("none");
            todo.setSchedule(schedule);
        }

        LocalDateTime now = LocalDateTime.now();
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);

        todoMapper.insert(todo);

        if (input.getAttachments() != null && !input.getAttachments().isEmpty()) {
            saveAttachments(todo.getId(), "todo", input.getAttachments());
        }

        tagSyncService.addTags(ownerId, todo.getTags());
        notificationService.notifyTodoCreated(todo.getId(), ownerId);
        return findById(todo.getId(), ownerId);
    }

    @Transactional
    public Todo update(String id, TodoInput input, String userId) {
        Todo existing = loadTodo(id);
        resourceAccess.requireTodoOwner(existing, userId);
        List<String> oldTags = existing.getTags() != null ? new ArrayList<>(existing.getTags()) : new ArrayList<>();

        existing.setTitle(input.getTitle());
        existing.setDescription(input.getDescription());
        if (input.getCompleted() != null) existing.setCompleted(input.getCompleted());
        if (input.getTags() != null) existing.setTags(input.getTags());
        if (input.getPriority() != null) existing.setPriority(input.getPriority());
        if (input.getIsFavorite() != null) existing.setFavorite(input.getIsFavorite());

        LocalDateTime previousReminder = existing.getReminder();
        LocalDateTime newReminder = parseDate(input.getReminder());
        existing.setDueDate(parseDate(input.getDueDate()));
        // Reminder change must clear notified_at, else findWithDueReminders never fires again
        if (!Objects.equals(previousReminder, newReminder)) {
            existing.setNotifiedAt(null);
        }
        existing.setReminder(newReminder);

        // Always apply channels when present in payload (incl. empty string to clear)
        if (input.getNotificationChannels() != null) {
            existing.setNotificationChannels(input.getNotificationChannels().isBlank() ? null : input.getNotificationChannels());
        }

        if (input.getSchedule() != null) {
            existing.setSchedule(convertToSchedule(input.getSchedule()));
        }

        existing.setUpdatedAt(LocalDateTime.now());
        todoMapper.update(existing);

        if (input.getAttachments() != null) {
            attachmentMapper.deleteByParent(id, "todo");
            saveAttachments(id, "todo", input.getAttachments());
        }

        tagSyncService.updateTags(existing.getOwnerId(), oldTags, existing.getTags());
        notificationService.notifyTodoUpdated(id, existing.getOwnerId());
        return findById(id, userId);
    }

    @Transactional
    public void delete(String id, boolean permanent, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoOwner(todo, userId);
        String ownerId = todo.getOwnerId();
        List<String> tags = todo.getTags();
        if (permanent) {
            attachmentMapper.deleteByParent(id, "todo");
            todoMapper.permanentDelete(id);
            tagSyncService.removeTagsIfUnused(ownerId, tags);
        } else {
            todoMapper.softDelete(id, LocalDateTime.now());
        }
        notificationService.notifyTodoDeleted(id, ownerId);
    }

    public Todo archive(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoOwner(todo, userId);
        todoMapper.archive(id);
        return findById(id, userId);
    }

    public Todo restore(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoOwner(todo, userId);
        todoMapper.restore(id);
        return findById(id, userId);
    }

    @Transactional
    public Todo shareWithUser(String todoId, String userIdToAdd, String currentOwnerId) {
        Todo todo = loadTodo(todoId);
        resourceAccess.requireTodoOwner(todo, currentOwnerId);

        List<String> sharedUsers = resourceAccess.parseSharedWith(todo.getSharedWith());
        if (!sharedUsers.contains(userIdToAdd)) {
            sharedUsers.add(userIdToAdd);
            String newSharedWith = toJsonArray(sharedUsers);
            todo.setSharedWith(newSharedWith);
            todoMapper.shareWithUser(todoId, newSharedWith);
        }

        return findById(todoId, currentOwnerId);
    }

    @Transactional
    public Todo unshareWithUser(String todoId, String userIdToRemove, String currentOwnerId) {
        Todo todo = loadTodo(todoId);
        resourceAccess.requireTodoOwner(todo, currentOwnerId);

        List<String> sharedUsers = resourceAccess.parseSharedWith(todo.getSharedWith());
        sharedUsers.remove(userIdToRemove);
        String newSharedWith = toJsonArray(sharedUsers);
        todo.setSharedWith(newSharedWith);
        todoMapper.shareWithUser(todoId, newSharedWith);

        return findById(todoId, currentOwnerId);
    }

    /**
     * Toggle completion. Recurring: done for current period only (log + next reminder).
     * Undo same period restores current slot. One-shot: simple flip.
     */
    @Transactional
    public Todo toggleComplete(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoRead(todo, userId);
        LocalDateTime now = Recurrence.nowUtc();
        persistRollover(todo, now);

        if (!Recurrence.isRecurring(todo)) {
            todo.setCompleted(!todo.isCompleted());
            todo.setUpdatedAt(now);
            todoMapper.update(todo);
            notificationService.notifyTodoUpdated(id, todo.getOwnerId());
            return todo;
        }

        if (Recurrence.isDoneThisPeriod(todo, now) || todo.isCompleted()) {
            return uncompleteRecurringOccurrence(todo, now);
        }
        return completeRecurringOccurrence(todo, now);
    }

    /**
     * Complete current period: log history, mark done, point reminder at next slot.
     */
    private Todo completeRecurringOccurrence(Todo todo, LocalDateTime now) {
        String repeat = Recurrence.repeatOf(todo);
        LocalDateTime slot = Recurrence.currentSlot(todo.getReminder(), repeat, now);

        TodoCompletionLog log = new TodoCompletionLog();
        log.setId(UUID.randomUUID().toString());
        log.setTodoId(todo.getId());
        log.setCompletedAt(now);
        log.setOccurrenceReminder(slot);
        log.setOccurrenceDueDate(todo.getDueDate());
        todoMapper.insertCompletionLog(log);

        LocalDateTime nextReminder = Recurrence.advance(slot, repeat);
        LocalDateTime nextDueDate = Recurrence.advanceDueDate(todo.getDueDate(), todo.getReminder(), nextReminder);

        todo.setLastCompletedAt(now);
        todo.setCompleted(true);
        todo.setUpdatedAt(now);

        if (nextReminder != null && Recurrence.pastEndDate(todo, nextReminder)) {
            todoMapper.update(todo);
            todoMapper.updateLastCompletedAt(todo.getId(), now);
            notificationService.notifyTodoUpdated(todo.getId(), todo.getOwnerId());
            return todo;
        }

        todo.setReminder(nextReminder);
        todo.setDueDate(nextDueDate);
        todo.setNotifiedAt(null);
        todoMapper.update(todo);
        todoMapper.updateLastCompletedAt(todo.getId(), now);
        notificationService.notifyTodoUpdated(todo.getId(), todo.getOwnerId());
        return todo;
    }

    private Todo uncompleteRecurringOccurrence(Todo todo, LocalDateTime now) {
        String repeat = Recurrence.repeatOf(todo);
        LocalDateTime slot = Recurrence.currentSlot(todo.getReminder(), repeat, now);
        LocalDateTime periodStart = slot.toLocalDate().atStartOfDay();

        todoMapper.deleteCompletionLogsSince(todo.getId(), periodStart);

        List<TodoCompletionLog> remaining = todoMapper.findCompletionLog(todo.getId());
        todo.setLastCompletedAt(remaining.isEmpty() ? null : remaining.get(0).getCompletedAt());
        todo.setCompleted(false);
        todo.setDueDate(Recurrence.advanceDueDate(todo.getDueDate(), todo.getReminder(), slot));
        todo.setReminder(slot);
        todo.setNotifiedAt(null);
        todo.setUpdatedAt(now);
        todoMapper.update(todo);
        notificationService.notifyTodoUpdated(todo.getId(), todo.getOwnerId());
        return todo;
    }

    private void rolloverOwnerRecurring(String ownerId) {
        LocalDateTime now = Recurrence.nowUtc();
        for (Todo todo : todoMapper.findRecurringActive(ownerId)) {
            persistRollover(todo, now);
        }
    }

    private boolean persistRollover(Todo todo, LocalDateTime now) {
        if (!Recurrence.rolloverIfNeeded(todo, now)) {
            return false;
        }
        todoMapper.update(todo);
        return true;
    }

    /**
     * Get completion history for a recurring todo.
     */
    public List<TodoCompletionLog> getCompletionLog(String todoId, String userId) {
        Todo todo = loadTodo(todoId);
        resourceAccess.requireTodoRead(todo, userId);
        return todoMapper.findCompletionLog(todoId);
    }

    static boolean isRecurring(Todo todo) {
        return Recurrence.isRecurring(todo);
    }

    private String toJsonArray(List<String> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private Todo.Schedule convertToSchedule(Map<String, Object> map) {
        if (map == null) return new Todo.Schedule();
        Todo.Schedule schedule = new Todo.Schedule();
        Object repeat = map.get("repeat");
        Object endDate = map.get("endDate");
        Object daysOfWeek = map.get("daysOfWeek");
        if (repeat instanceof String) schedule.setRepeat((String) repeat);
        if (endDate instanceof String) {
            String dateStr = (String) endDate;
            try {
                schedule.setEndDate(java.time.Instant.parse(dateStr).atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
            } catch (Exception e) {
                try {
                    schedule.setEndDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e2) {
                    try {
                        schedule.setEndDate(LocalDate.parse(dateStr).atStartOfDay());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (daysOfWeek instanceof List<?> list) {
            List<Integer> days = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number n) {
                    days.add(n.intValue());
                }
            }
            schedule.setDaysOfWeek(days);
        }
        // weekdays → Mon–Fri if not provided
        if ("weekdays".equalsIgnoreCase(schedule.getRepeat())
                && (schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().isEmpty())) {
            schedule.setDaysOfWeek(List.of(1, 2, 3, 4, 5));
        }
        if ("none".equalsIgnoreCase(schedule.getRepeat())) {
            schedule.setEndDate(null);
            schedule.setDaysOfWeek(null);
        }
        return schedule;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            java.time.Instant instant = java.time.Instant.parse(dateStr);
            return LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(dateStr).atStartOfDay();
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private void saveAttachments(String parentId, String parentType, List<Map<String, Object>> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> attachmentData : attachments) {
            xyz.crearts.note.keeper.model.Attachment attachment = new xyz.crearts.note.keeper.model.Attachment();
            attachment.setId(UUID.randomUUID().toString());
            attachment.setParentId(parentId);
            attachment.setParentType(parentType);

            Object name = attachmentData.get("name");
            Object size = attachmentData.get("size");
            Object type = attachmentData.get("type");
            Object url = attachmentData.get("url");

            if (name instanceof String) attachment.setName((String) name);
            if (size instanceof Number) attachment.setSize(((Number) size).longValue());
            if (type instanceof String) attachment.setType((String) type);
            if (url instanceof String) attachment.setUrl((String) url);

            attachment.setUploadedAt(now);
            attachmentMapper.insert(attachment);
        }
    }
}
